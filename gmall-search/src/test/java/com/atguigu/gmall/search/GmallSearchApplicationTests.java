package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GoodsRepository repository;

    @Test
    void contextLoads() {
        this.restTemplate.createIndex(Goods.class);
        this.restTemplate.putMapping(Goods.class);

        Integer pageNum = 1;
        Integer pageSize = 100;

        do {
            // 分批查询spu
            PageParamVo paramVo = new PageParamVo();
            paramVo.setPageNum(pageNum);
            paramVo.setPageSize(pageSize);
            ResponseVo<List<SpuEntity>> listResponseVo = this.pmsClient.querySpuByPageJson(paramVo);
            List<SpuEntity> spuEntities = listResponseVo.getData();
            if (CollectionUtils.isEmpty(spuEntities)){
                continue;
            }

            // 遍历当前页的所有spu，查询spu下的所有sku，转化成goods对象集合
            spuEntities.forEach(spuEntity -> {
                // 查询出spu下所有的sku集合
                ResponseVo<List<SkuEntity>> skusResponseVo = this.pmsClient.querySkusBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skusResponseVo.getData();
                if (!CollectionUtils.isEmpty(skuEntities)){
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
                        goods.setCreateTime(spuEntity.getCreateTime());

                        // 库存相关信息
                        ResponseVo<List<WareSkuEntity>> wareSkusResponseVO = this.wmsClient.queryWareSkusBySkuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareSkusResponseVO.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)){
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                            goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                        }

                        // 检索属性和值
                        List<SearchAttrValueVo> attrValueVos = new ArrayList<>();
                        // skuAttrValueEntity
                        ResponseVo<List<SkuAttrValueEntity>> skuAttrsResponseVo = this.pmsClient.querySearchSkuAttrValuesByCidAndSkuId(skuEntity.getCatagoryId(), skuEntity.getId());
                        List<SkuAttrValueEntity> searchSkuAttrValueEntities = skuAttrsResponseVo.getData();
                        if (!CollectionUtils.isEmpty(searchSkuAttrValueEntities)){
                            attrValueVos.addAll(searchSkuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));
                        }

                        // spuAttrValueEntity
                        ResponseVo<List<SpuAttrValueEntity>> spuAttrsResponseVo = this.pmsClient.querySearchSpuAttrValuesByCidAndSpuId(skuEntity.getCatagoryId(), spuEntity.getId());
                        List<SpuAttrValueEntity> searchSpuAttrValueEntities = spuAttrsResponseVo.getData();
                        if (!CollectionUtils.isEmpty(searchSpuAttrValueEntities)){
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
            });

            pageSize = spuEntities.size();
            pageNum++;
        } while (pageSize == 100);
    }

}
