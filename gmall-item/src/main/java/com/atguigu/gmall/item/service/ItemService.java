package com.atguigu.gmall.item.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    public ItemVo loadData(Long skuId) {
        ItemVo itemVo = new ItemVo();

        // 1.查询sku相关信息
        ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
        SkuEntity skuEntity = skuEntityResponseVo.getData();
        if (skuEntity == null) {
            return null;
        }
        itemVo.setSkuId(skuEntity.getId());
        itemVo.setTitle(skuEntity.getTitle());
        itemVo.setSubTitle(skuEntity.getSubtitle());
        itemVo.setPrice(skuEntity.getPrice());
        itemVo.setWeight(skuEntity.getWeight());
        itemVo.setDefaultImage(skuEntity.getDefaultImage());

        // 2.查询分类信息
        ResponseVo<List<CategoryEntity>> categoryResponseVo = this.pmsClient.queryCategoriesByCid3(skuEntity.getCatagoryId());
        List<CategoryEntity> categoryEntities = categoryResponseVo.getData();
        itemVo.setCategories(categoryEntities);

        // 3.查询品牌信息
        ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
        BrandEntity brandEntity = brandEntityResponseVo.getData();
        if (brandEntity != null){
            itemVo.setBrandId(brandEntity.getId());
            itemVo.setBrandName(brandEntity.getName());
        }

        // 4.查询spu相关信息
        ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
        SpuEntity spuEntity = spuEntityResponseVo.getData();
        if (spuEntity != null) {
            itemVo.setSpuId(spuEntity.getId());
            itemVo.setSpuName(spuEntity.getName());
        }

        // 5.查询sku图片列表
        ResponseVo<List<SkuImagesEntity>> skuImagesResponseVo = this.pmsClient.queryImagesBySkuId(skuId);
        List<SkuImagesEntity> skuImagesEntities = skuImagesResponseVo.getData();
        itemVo.setImages(skuImagesEntities);

        // 6.查询sku营销信息
        ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.queryItemSalesBySkuId(skuId);
        List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
        itemVo.setSales(itemSaleVos);

        // 7.查询库存信息
        ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(skuId);
        List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
        if (!CollectionUtils.isEmpty(wareSkuEntities)){
            itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
        }

        // 8.查询spu所有的销售属性
        ResponseVo<List<SaleAttrValueVo>> saleAttrsResponseVo = this.pmsClient.querySaleAttrValuesBySpuId(skuEntity.getSpuId());
        List<SaleAttrValueVo> saleAttrValueVos = saleAttrsResponseVo.getData();
        itemVo.setSaleAttrs(saleAttrValueVos);


        // 9.查询sku的销售属性
        ResponseVo<List<SkuAttrValueEntity>> skuAttrResponseVo = this.pmsClient.querySkuAttrValueBySkuId(skuId);
        List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
            itemVo.setSaleAttr(skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue)));
        }

        // 10.查询销售属性组合和skuId的映射关系
        ResponseVo<String> stringResponseVo = this.pmsClient.querySkuIdMappingSaleAttrValueBySpuId(skuEntity.getSpuId());
        String json = stringResponseVo.getData();
        itemVo.setSkuJsons(json);

        // 11.查询商品描述信息
        ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
        SpuDescEntity descEntity = spuDescEntityResponseVo.getData();
        if (descEntity != null) {
            String[] urls = StringUtils.split(descEntity.getDecript(), ",");
            itemVo.setSpuImages(Arrays.asList(urls));
        }

        // 12.查询组及组下的规格参数和值
        ResponseVo<List<ItemGroupVo>> groupResponseVo = this.pmsClient.queryGroupsWithAttrAndValueByCidAndSpuIdAndSkuId(skuEntity.getCatagoryId(), skuEntity.getSpuId(), skuId);
        List<ItemGroupVo> groups = groupResponseVo.getData();
        itemVo.setGroups(groups);

        return itemVo;
    }
}
