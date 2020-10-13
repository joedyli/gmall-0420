package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {

    // 包含一、二、三级分类元素 Y
    private List<CategoryEntity> categories;

    // 品牌 Y
    private Long brandId;
    private String brandName;

    // spu信息 Y
    private Long spuId;
    private String spuName;

    // sku信息 Y
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private String defaultImage;
    private Integer weight;

    // 图片列表 Y
    private List<SkuImagesEntity> images;

    // 营销信息 Y
    private List<ItemSaleVo> sales;

    // 库存信息 Y
    private Boolean store = false;

    // spu下所有sku的营销属性信息： Y
    // [{attrId: 8, attrName: '颜色', attrValues: ['白色', '黑色']},
    // {attrId: 9, attrName: '内存', attrValues: ['8G', '12G']},
    // {attrId: 10, attrName: '存储', attrValues: ['128G', '256G', '512G']}]
    private List<SaleAttrValueVo> saleAttrs;

    // 获取当前sku的销售属性： {8: '白色'，9: '8G', 10: '512G'} Y
    private Map<Long, String> saleAttr;

    // 销售属性组合和skuId映射关系 Y
    // {'白色, 8G, 128G': 10, '黑色, 8G, 128G': 11}
    private String skuJsons;

    // 商品描述 Y
    private List<String> spuImages;

    private List<ItemGroupVo> groups;
}
