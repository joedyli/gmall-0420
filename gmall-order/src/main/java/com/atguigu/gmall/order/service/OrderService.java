package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIXE = "order:token:";

    public OrderConfirmVo confirm() {

        OrderConfirmVo confirmVo = new OrderConfirmVo();

        // 获取用户的登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        // 获取用户的地址列表
        ResponseVo<List<UserAddressEntity>> addressesResponseVo = this.umsClient.queryAddressesByUserId(userId);
        List<UserAddressEntity> userAddressEntities = addressesResponseVo.getData();
        confirmVo.setAddresses(userAddressEntities);

        // 商品详情列表
        ResponseVo<List<Cart>> cartsResponseVo = this.cartClient.queryCheckedCartsByUserId(userId);
        List<Cart> carts = cartsResponseVo.getData();
        if (CollectionUtils.isEmpty(carts)){
            throw new OrderException("您没有选中的购物车信息！");
        }
        List<OrderItemVo> orderItemVos = carts.stream().map(cart -> {
            OrderItemVo itemVo = new OrderItemVo();
            itemVo.setSkuId(cart.getSkuId());
            itemVo.setCount(cart.getCount());

            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null){
                itemVo.setTitle(skuEntity.getTitle());
                itemVo.setDefaultImage(skuEntity.getDefaultImage());
                itemVo.setWeight(new BigDecimal(skuEntity.getWeight()));
                itemVo.setPrice(skuEntity.getPrice());
            }

            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySkuAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            itemVo.setSaleAttrs(skuAttrValueEntities);

            ResponseVo<List<ItemSaleVo>> saleResponseVo = this.smsClient.queryItemSalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = saleResponseVo.getData();
            itemVo.setSales(itemSaleVos);

            return itemVo;
        }).collect(Collectors.toList());
        confirmVo.setItems(orderItemVos);

        // 根据用户id查询用户
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity != null){
            confirmVo.setBounds(userEntity.getIntegration());
        }

        // 生成一个orderToken，放入redis一份
        String orderToken = IdWorker.getTimeId();
        confirmVo.setOrderToken(orderToken);
        this.redisTemplate.opsForValue().set(KEY_PREFIXE + orderToken, orderToken);

        return confirmVo;
    }

    public OrderEntity submit(OrderSubmitVo submitVo) {

        // 1.防重
        String orderToken = submitVo.getOrderToken();
        if (StringUtils.isEmpty(orderToken)){
            throw new OrderException("非法提交！");
        }
        String script = "if(redis.call('get', KEYS[1])==ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIXE + orderToken), orderToken);
        if (!flag){
            throw new OrderException("请不要重复提交！");
        }

        // 2.验总价
        BigDecimal totalPrice = submitVo.getTotalPrice();
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("请选择要购买的商品！");
        }
        // 获取实时总价
        BigDecimal currentTotalPrice = items.stream().map(itemVo -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(itemVo.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                // 每条购物记录的小计价格
                return skuEntity.getPrice().multiply(itemVo.getCount());
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();
        if (currentTotalPrice.compareTo(totalPrice) != 0){
            throw new OrderException("页面已过期请刷新后重试！");
        }

        // 3.验库存并锁定库存
        List<SkuLockVo> skuLockVos = items.stream().map(item -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> wareskuResponseVo = this.wmsClient.checkAndLock(skuLockVos, orderToken);
        List<SkuLockVo> skuLockVoList = wareskuResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuLockVoList)){
            throw new OrderException(JSON.toJSONString(skuLockVoList));
        }

        // 4.下单
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        OrderEntity orderEntity = null;
        try {
            ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.saveOrder(submitVo, userId);
            orderEntity = orderEntityResponseVo.getData();
        } catch (Exception e) {
            e.printStackTrace();
            // 标记订单是无效订单，并立马解锁库存
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.disable", orderToken);
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.unlock", orderToken);
        }


        // 5.删除购物车中对应的商品：异步
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("skuIds", JSON.toJSONString(skuIds));
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "cart.delete", map);

        return orderEntity;
    }
}
