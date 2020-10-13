package com.atguigu.gmall.pms.service.impl;

import jdk.nashorn.internal.ir.IfNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategoriesByPid(Long pid) {

        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();

        if (pid != -1){
            wrapper.eq("parent_id", pid);
        }

        return this.list(wrapper);
    }

    @Override
    public List<CategoryEntity> queryCategoryLvl2WithSubsByPid(Long pid) {

        List<CategoryEntity> categoryEntities = this.categoryMapper.queryCategoryByPid(pid);
        return categoryEntities;
    }

    @Override
    public List<CategoryEntity> queryCategoriesByCid3(Long cid3) {

        // 查询3级分类
        CategoryEntity lvl3CategoryEntity = this.categoryMapper.selectById(cid3);

        // 查询2级分类
        CategoryEntity lvl2CategoryEntity = this.categoryMapper.selectById(lvl3CategoryEntity.getParentId());

        // 查询1级分类
        CategoryEntity lvl1CategoryEntity = this.categoryMapper.selectById(lvl2CategoryEntity.getParentId());

        return Arrays.asList(lvl1CategoryEntity, lvl2CategoryEntity, lvl3CategoryEntity);
    }

}
