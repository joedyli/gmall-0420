package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/**
 * https://search.gmall.com/search?keyword=手机&brandId=1,2,3&cid3=225,250&props=4:8G-12G&props=5:128G-256G-521G&sort=1&priceFrom=1000
 *  &priceTo=2000&store=true&pageNum=1
 */
@Data
public class SearchParamVo {

    // 搜索关键字
    private String keyword;

    // 接收品牌id的过滤条件
    private List<Long> brandId;

    // 接收分类的过滤条件
    private List<Long> cid3;

    // 接收规格参数的过滤条件 5:128G-256G-521G
    private List<String> props;

    // 排序：1-价格升序 2-价格降序 3-新品降序 4-销量降序
    private Integer sort;

    // 价格区间
    private Double priceFrom;
    private Double priceTo;

    // 是否有货
    private Boolean store;

    // 分页数据
    private Long total;
    private Integer pageNum;
    private final Integer pageSize = 20;
}
