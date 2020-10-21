package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 订单
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2020-10-19 15:24:12
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    public int updateStatus(@Param("orderToken") String orderToken, @Param("expect") Integer expect, @Param("target") Integer target);
}
