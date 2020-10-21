package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.service.OrderItemService;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private OrderItemService itemService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId) {

        // 1.保存订单表
        OrderEntity orderEntity = new OrderEntity();
        // 查询用户信息
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity != null) {
            orderEntity.setUserId(userId);
            orderEntity.setUsername(userEntity.getUsername());
        }

        orderEntity.setOrderSn(submitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        // TODO:各种优惠抵扣金额
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(1);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());
        UserAddressEntity address = submitVo.getAddress();
        if (address != null) {
            orderEntity.setReceiverName(address.getName());
            orderEntity.setReceiverPhone(address.getPhone());
            orderEntity.setReceiverCity(address.getCity());
            orderEntity.setReceiverPostCode(address.getPostCode());
            orderEntity.setReceiverProvince(address.getProvince());
            orderEntity.setReceiverRegion(address.getRegion());
            orderEntity.setReceiverAddress(address.getAddress());
        }
        orderEntity.setConfirmStatus(0);
        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(submitVo.getBounds());

        this.save(orderEntity);
        Long id = orderEntity.getId();

        // 2.保存订单详情表
        List<OrderItemVo> items = submitVo.getItems();
        if (!CollectionUtils.isEmpty(items)){
            itemService.saveBatch(items.stream().map(item -> {
                OrderItemEntity itemEntity = new OrderItemEntity();

                itemEntity.setOrderId(id);
                itemEntity.setOrderSn(submitVo.getOrderToken());

                // 根据skuId查询sku
                ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
                SkuEntity skuEntity = skuEntityResponseVo.getData();
                if (skuEntity != null) {
                    itemEntity.setSkuId(skuEntity.getId());
                    itemEntity.setSkuName(skuEntity.getName());
                    itemEntity.setSkuQuantity(item.getCount().intValue());
                    itemEntity.setSkuPrice(skuEntity.getPrice());
                    itemEntity.setSkuPic(skuEntity.getDefaultImage());
                    ResponseVo<List<SkuAttrValueEntity>> listResponseVo = this.pmsClient.querySkuAttrValueBySkuId(item.getSkuId());
                    List<SkuAttrValueEntity> skuAttrValueEntities = listResponseVo.getData();
                    itemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrValueEntities));
                    itemEntity.setCategoryId(skuEntity.getCatagoryId());

                    ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                    BrandEntity brandEntity = brandEntityResponseVo.getData();
                    if (brandEntity != null) {
                        itemEntity.setSpuBrand(brandEntity.getName());
                    }

                    ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
                    SpuEntity spuEntity = spuEntityResponseVo.getData();
                    if (spuEntity != null) {
                        itemEntity.setSpuId(spuEntity.getId());
                        itemEntity.setSpuName(spuEntity.getName());
                    }

                    ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
                    SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
                    if (spuDescEntity != null) {
                        itemEntity.setSpuPic(spuDescEntity.getDecript());
                    }

                    // TODO：根据skuId查询积分优惠
                }


                return itemEntity;
            }).collect(Collectors.toList()));
        }

        // 发送消息给mq
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.ttl", submitVo.getOrderToken());

        return orderEntity;
    }

}
