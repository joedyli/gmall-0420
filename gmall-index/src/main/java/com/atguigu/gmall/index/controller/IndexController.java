package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping({"/", "index"})
    public String toIndex(Model model){
        // 查询一级分类
        List<CategoryEntity> categoryEntities = this.indexService.queryLvl1Categories();
        model.addAttribute("categories", categoryEntities);

        // TODO: 查询各种广告
        return "index";
    }

    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLvl2WithSubsByPid(@PathVariable("pid")Long pid){
        List<CategoryEntity> categoryEntities = this.indexService.queryLvl2WithSubsByPid(pid);
        return ResponseVo.ok(categoryEntities);
    }

    @GetMapping("index/test/lock")
    @ResponseBody
    public ResponseVo testLock() throws InterruptedException {
        this.indexService.testLock();
        return ResponseVo.ok();
    }

    @GetMapping("index/test/write")
    @ResponseBody
    public ResponseVo testWrite(){
        this.indexService.testWrite();
        return ResponseVo.ok();
    }

    @GetMapping("index/test/read")
    @ResponseBody
    public ResponseVo testRead(){
        this.indexService.testRead();
        return ResponseVo.ok();
    }

    @GetMapping("index/test/latch")
    @ResponseBody
    public ResponseVo testLatch() throws InterruptedException {
        String msg = this.indexService.testLatch();
        return ResponseVo.ok(msg);
    }

    @GetMapping("index/test/countDown")
    @ResponseBody
    public ResponseVo testCountDown(){
        String msg = this.indexService.testCountDown();
        return ResponseVo.ok(msg);
    }
}
