package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class CartListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartMapper cartMapper;

    private static final String PRICE_PREFIX = "cart:price:";

    private static final String KEY_PREFIX = "cart:info:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART_PMS_QUEUE", durable = "true"),
            exchange = @Exchange(value = "PMS_SPU_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        if (spuId == null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        ResponseVo<List<SkuEntity>> responseVo = this.pmsClient.querySkusBySpuId(spuId);
        List<SkuEntity> skuEntities = responseVo.getData();
        if (!CollectionUtils.isEmpty(skuEntities)){
            skuEntities.forEach(skuEntity -> {
                String price = this.redisTemplate.opsForValue().get(PRICE_PREFIX + skuEntity.getId());
                if (StringUtils.isNoneBlank(price)){
                    this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuEntity.getId(), skuEntity.getPrice().toString());
                }
            });
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART_DELETE_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"cart.delete"}
    ))
    public void delete(Map<String, Object> map, Channel channel, Message message) throws IOException {
        if (CollectionUtils.isEmpty(map)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        String userId = map.get("userId").toString();
        String skuIdString = map.get("skuIds").toString();
        List<String> skuIds = JSON.parseArray(skuIdString, String.class);

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        hashOps.delete(skuIds.toArray());

        this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId).in("sku_id", skuIds));
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
