package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface GmallPmsApi {

    @PostMapping("pms/spu/json")
    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkusBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/skuattrvalue/search/{cid}/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchSkuAttrValuesByCidAndSkuId(
            @PathVariable("cid")Long cid, @PathVariable("skuId")Long skuId
    );

    @GetMapping("pms/spuattrvalue/search/{cid}/{spuId}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchSpuAttrValuesByCidAndSpuId(
            @PathVariable("cid")Long cid, @PathVariable("spuId")Long spuId
    );
}
