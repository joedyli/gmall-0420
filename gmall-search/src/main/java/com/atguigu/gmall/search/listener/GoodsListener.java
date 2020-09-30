package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GoodsRepository repository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "SEARCH_ADD_QUEUE", durable = "true"),
            exchange = @Exchange(value = "PMS_ITEM_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    ))
    public void listener(Long spuId, Channel channel, Message message){
        // 查询出spu下所有的sku集合
        ResponseVo<List<SkuEntity>> skusResponseVo = this.pmsClient.querySkusBySpuId(spuId);
        List<SkuEntity> skuEntities = skusResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuEntities)) {
            // 转化成goods集合
            List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                Goods goods = new Goods();

                // sku相关信息
                goods.setSkuId(skuEntity.getId());
                goods.setTitle(skuEntity.getTitle());
                goods.setSubTitle(skuEntity.getSubtitle());
                goods.setPrice(skuEntity.getPrice().doubleValue());
                goods.setDefaultImage(skuEntity.getDefaultImage());

                // 品牌相关信息
                ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                BrandEntity brandEntity = brandEntityResponseVo.getData();
                if (brandEntity != null) {
                    goods.setBrandId(brandEntity.getId());
                    goods.setBrandName(brandEntity.getName());
                    goods.setLogo(brandEntity.getLogo());
                }

                // 分类相关信息
                ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(skuEntity.getCatagoryId());
                CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                if (categoryEntity != null) {
                    goods.setCategoryId(categoryEntity.getId());
                    goods.setCategoryName(categoryEntity.getName());
                }

                // spu相关信息
                ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(spuId);
                SpuEntity spuEntity = spuEntityResponseVo.getData();
                if (spuEntity != null){
                    goods.setCreateTime(spuEntity.getCreateTime());
                }

                // 库存相关信息
                ResponseVo<List<WareSkuEntity>> wareSkusResponseVO = this.wmsClient.queryWareSkusBySkuId(skuEntity.getId());
                List<WareSkuEntity> wareSkuEntities = wareSkusResponseVO.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                    goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                }

                // 检索属性和值
                List<SearchAttrValueVo> attrValueVos = new ArrayList<>();
                // skuAttrValueEntity
                ResponseVo<List<SkuAttrValueEntity>> skuAttrsResponseVo = this.pmsClient.querySearchSkuAttrValuesByCidAndSkuId(skuEntity.getCatagoryId(), skuEntity.getId());
                List<SkuAttrValueEntity> searchSkuAttrValueEntities = skuAttrsResponseVo.getData();
                if (!CollectionUtils.isEmpty(searchSkuAttrValueEntities)) {
                    attrValueVos.addAll(searchSkuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }

                // spuAttrValueEntity
                ResponseVo<List<SpuAttrValueEntity>> spuAttrsResponseVo = this.pmsClient.querySearchSpuAttrValuesByCidAndSpuId(skuEntity.getCatagoryId(), spuId);
                List<SpuAttrValueEntity> searchSpuAttrValueEntities = spuAttrsResponseVo.getData();
                if (!CollectionUtils.isEmpty(searchSpuAttrValueEntities)) {
                    attrValueVos.addAll(searchSpuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity, searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }
                goods.setSearchAttrs(attrValueVos);

                return goods;
            }).collect(Collectors.toList());

            // 批量导入到es
            this.repository.saveAll(goodsList);
        }
    }
}
