package com.atguigu.gmall.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.service.SpuService;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

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

}
