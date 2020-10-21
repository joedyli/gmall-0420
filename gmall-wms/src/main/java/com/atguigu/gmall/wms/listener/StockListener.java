package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

@Component
public class StockListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    private static final String KEY_PREFIX = "stock:lock:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK_UNLOCK_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unlock(String orderToken, Channel channel, Message message) throws IOException {

        String lockString = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        // 如果锁定库存信息为空，不用作任何处理
        if (StringUtils.isBlank(lockString)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return ;
        }

        // 反序列化，再 判断锁定库存信息是否为空
        List<SkuLockVo> skuLockVos = JSON.parseArray(lockString, SkuLockVo.class);
        if (CollectionUtils.isEmpty(skuLockVos)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return ;
        }

        // 遍历锁定信息解锁库存
        skuLockVos.forEach(skuLockVo -> {
            this.wareSkuMapper.unlock(skuLockVo.getWareSkuId(), skuLockVo.getCount());
        });

        // 防止重复解锁库存
        this.redisTemplate.delete(KEY_PREFIX + orderToken);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK_MINUS_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void minus(String orderToken, Channel channel, Message message) throws IOException {

        // 获取库存锁定信息
        String stockJson = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if (StringUtils.isNotBlank(stockJson)){
            List<SkuLockVo> skuLockVos = JSON.parseArray(stockJson, SkuLockVo.class);
            skuLockVos.forEach(lockVo -> {
                this.wareSkuMapper.minus(lockVo.getWareSkuId(), lockVo.getCount());
            });
        }

        // 防止解锁库存的发生，删除库存锁定信息
        this.redisTemplate.delete(KEY_PREFIX + orderToken);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
