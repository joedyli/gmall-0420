package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.annotation.TableField;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper descMapper;

    @Autowired
    private SpuAttrValueService baseAttrService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService imagesService;

    @Autowired
    private SkuAttrValueService attrValueService;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private SpuDescService descService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByCidPage(Long cid, PageParamVo pageParamVo) {

        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();

        // 分类id不为0，说明要查本类，如果为0，说明查询全站
        if (cid != 0) {
            wrapper.eq("category_id", cid);
        }

        // 查询关键字
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)) {
            wrapper.and(t -> t.eq("id", key).or().like("name", key));
        }


        // 使用mp的分页查询方法
        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }

    @Override
    //@Transactional(rollbackFor = Exception.class, timeout = 3, readOnly = true)
    @GlobalTransactional
    public void bigSave(SpuVo spu) throws FileNotFoundException {
        // 1.保存spu相关信息
        // 1.1.保存pms_spu
        Long spuId = saveSpu(spu);

        // 1.2.保存pms_spu_desc
        //this.saveSpuDesc(spu, spuId);
        this.descService.saveSpuDesc(spu, spuId);

//        try {
//            TimeUnit.SECONDS.sleep(4);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

//        int i = 1/0;
//        new FileInputStream("xxxx");

        // 1.3.保存pms_spu_attr_value
        this.saveBaseAttrs(spu, spuId);

        // 2.保存sku相关信息
        this.saveSkus(spu, spuId);


        this.rabbitTemplate.convertAndSend("PMS_ITEM_EXCHANGE", "item.insert", spuId);
        //int i = 1/0;
    }

    private void saveSkus(SpuVo spu, Long spuId) {
        List<SkuVo> skus = spu.getSkus();
        if (CollectionUtils.isEmpty(skus)){
            return ;
        }
        skus.forEach(skuVo -> {
            // 2.1.保存pms_sku
            skuVo.setSpuId(spuId);
            skuVo.setCatagoryId(spu.getCategoryId());
            skuVo.setBrandId(spu.getBrandId());
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)){
                skuVo.setDefaultImage(StringUtils.isNotBlank(skuVo.getDefaultImage()) ? skuVo.getDefaultImage(): images.get(0));
            }
            this.skuMapper.insert(skuVo);
            Long skuId = skuVo.getId();

            // 2.2.保存pms_sku_images
            if (!CollectionUtils.isEmpty(images)){
                imagesService.saveBatch(images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setSort(0);
                    if (StringUtils.equals(image, skuVo.getDefaultImage())){
                        skuImagesEntity.setDefaultStatus(1);
                    }
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }

            // 2.3.保存pms_sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)){
                saleAttrs.forEach(attr -> {
                    attr.setSkuId(skuId);
                    attr.setSort(0);
                });
                this.attrValueService.saveBatch(saleAttrs);
            }

            // 3.保存sku营销信息
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo, skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.smsClient.saveSales(skuSaleVo);
        });
    }

    private void saveBaseAttrs(SpuVo spu, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){
//            List<SpuAttrValueEntity> spuAttrValueEntities = new ArrayList<>();
//            baseAttrs.forEach(spuAttrValueVo -> {
//                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
//                BeanUtils.copyProperties(spuAttrValueVo, spuAttrValueEntity);
//                spuAttrValueEntity.setSpuId(spuId);
//                spuAttrValueEntity.setSort(0);
//                spuAttrValueEntities.add(spuAttrValueEntity);
//            });

            this.baseAttrService.saveBatch(baseAttrs.stream().map(spuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrValueVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                spuAttrValueEntity.setSort(0);
                return spuAttrValueEntity;
            }).collect(Collectors.toList()));
        }
    }

    private Long saveSpu(SpuVo spu) {
        spu.setCreateTime(new Date());
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        return spu.getId();
    }

}

class Test{
    public static void main(String[] args) {

        List<User> users = Arrays.asList(
            new User(1l, "柳岩", 20),
            new User(2l, "马苏", 21),
            new User(3l, "马蓉", 22),
            new User(4l, "小鹿", 23),
            new User(5l, "柏芝", 24),
            new User(6l, "凤姐", 25),
            new User(7l, "大幂幂", 26)
        );

        List<Integer> aa = Arrays.asList(1, 2, 3, 4, 5, 6);

        // 集合转化map 求和reduce 过滤filter
        //System.out.println(aa.stream().reduce((a, b) -> a + b).get());
        System.out.println(users.stream().map(User::getAge).reduce((a, b) -> a + b).get());

        System.out.println(users.stream().filter(user -> user.getAge() > 22).collect(Collectors.toList()));


        List<Person> persons = users.stream().map(user -> {
            Person person = new Person();
            person.setId(user.getId());
            person.setUserName(user.getName());
            person.setAge(user.getAge());
            return person;
        }).collect(Collectors.toList());
        System.out.println(persons);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
class User{
    private Long id;
    private String name;
    private Integer age;
}

@Data
@ToString
class Person{
    private Long id;
    private String userName;
    private Integer age;
}
