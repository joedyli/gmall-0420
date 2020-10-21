package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {

    private List<UserAddressEntity> addresses;

    private List<OrderItemVo> items;

    private Integer bounds;

    private String orderToken; // 防重/保证提交订单接口的幂等性

}
