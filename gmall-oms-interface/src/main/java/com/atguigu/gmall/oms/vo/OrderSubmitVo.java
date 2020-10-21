package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {

    private String orderToken; // 防重
    private UserAddressEntity address;
    private Integer payType;
    private String deliveryCompany;
    private List<OrderItemVo> items;

    private Integer bounds;
    private BigDecimal totalPrice; // 验价

    // TODO:发票信息 买家留言
}
