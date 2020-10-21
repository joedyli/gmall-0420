package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @GetMapping("pay.html")
    public String toPay(@RequestParam("orderToken")String orderToken, Model model){

        OrderEntity orderEntity = this.paymentService.queryOrderByToken(orderToken);
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        if (orderEntity == null || orderEntity.getUserId() != userId || orderEntity.getStatus() != 0){
            throw new OrderException("非法参数");
        }

        model.addAttribute("orderEntity", orderEntity);
        return "pay";
    }

    @GetMapping("alipay.html")
    @ResponseBody // 以其他视图形式展示方法返回结果集，只是经常用来响应json
    public String toAlipay(@RequestParam("orderToken")String orderToken){
        OrderEntity orderEntity = this.paymentService.queryOrderByToken(orderToken);
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        if (orderEntity == null || orderEntity.getUserId() != userId || orderEntity.getStatus() != 0){
            throw new OrderException("非法参数");
        }

        try {
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderEntity.getOrderSn());
            payVo.setTotal_amount("0.01"); // 只能保留两位小数
            payVo.setSubject("谷粒商城支付订单");

            // 生成支付对账记录
            Long id = this.paymentService.savePaymentInfo(orderEntity);
            payVo.setPassback_params(id.toString());

            return this.alipayTemplate.pay(payVo);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 支付宝支付同步回调接口
     * 仅仅跳转到支付成功页面即可
     * @return
     */
    @GetMapping("pay/ok")
    public String payOk(PayAsyncVo payAsyncVo){
        String out_trade_no = payAsyncVo.getOut_trade_no();
        // 查询订单，回显订单信息
        return "paysuccess";
    }

    /**
     * 异步回调接口：修改订单状态并减库存
     * @return
     */
    @PostMapping("pay/success")
    @ResponseBody
    public String paySuccess(PayAsyncVo payAsyncVo){
        System.out.println("异步回调成功====================================" + payAsyncVo);

        // 1.验签：失败直接返回failure
        Boolean flag = this.alipayTemplate.checkSignature(payAsyncVo);
        if (!flag){
            return "failure";
        }

        // 2.校验业务参数：app_id、out_trade_no、total_amount
        // 取payAsyncVo中的参数 和 数据库中的参数比较
        String app_id = payAsyncVo.getApp_id();
        String out_trade_no = payAsyncVo.getOut_trade_no();
        String total_amount = payAsyncVo.getTotal_amount();
        String payId = payAsyncVo.getPassback_params();
        // 获取数据库中对账信息
        PaymentInfoEntity paymentInfoEntity = this.paymentService.queryById(payId);
        if (!StringUtils.equals(app_id, alipayTemplate.getApp_id())
                || !StringUtils.equals(out_trade_no, paymentInfoEntity.getOutTradeNo())
                || paymentInfoEntity.getTotalAmount().compareTo(new BigDecimal(total_amount)) != 0
        ){
            return "failure";
        }

        // 3.校验支付状态码：TRADE_SUCCESS
        if (!StringUtils.equals(payAsyncVo.getTrade_status(), "TRADE_SUCCESS")){
            return "failure";
        }

        // 4.更新对账表
        if (this.paymentService.update(payAsyncVo) == 1){
            // 5.发送消息给oms更新订单状态，订单状态修改成功之后减库存
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.success", out_trade_no);
        }

        // 6.响应数据，高速支付宝处理成功
        return "success";
    }


    @GetMapping("seckill/{skuId}")
    @ResponseBody
    public ResponseVo<Object> seckill(@PathVariable("skuId")Long skuId){

        RLock lock = this.redissonClient.getLock("sec:lock:" + skuId);
        lock.lock();

        try {
            String stockString = this.redisTemplate.opsForValue().get("sec:kill:" + skuId);
            if (StringUtils.isBlank(stockString) || Integer.parseInt(stockString) == 0){
                throw new OrderException("手慢了，下次再来！");
            }

            this.redisTemplate.opsForValue().decrement("sec:kill:" + skuId);

            // 发送消息给mq，异步减库存，创建订单
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(skuId);
            skuLockVo.setCount(1);
            String orderToken = IdWorker.getTimeId();
            skuLockVo.setOrderToken(orderToken);
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "seckill.success", skuLockVo);

            RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("sec:latch:" + orderToken);
            countDownLatch.trySetCount(1);

            return ResponseVo.ok(orderToken);
        } finally {
            lock.unlock();
        }
    }

    @GetMapping("seckill/order/{orderToken}")
    public ResponseVo<OrderEntity> queryOrder(@PathVariable("orderToken")String orderToken) throws InterruptedException {

        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("sec:latch:" + orderToken);
        countDownLatch.await();
        // TODO: 根据订单编号查询订单

        return ResponseVo.ok(null);
    }
}
