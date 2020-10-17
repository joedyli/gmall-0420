package com.atguigu.gmall.scheduling.job;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import org.springframework.stereotype.Component;

@Component
public class MyJobHandler {

    /**
     * 方法返回值ReturnT对象
     * 通过调度中心传递param参数
     * @param param
     * @return
     */
    @XxlJob("myJobHandler")
    public ReturnT<String> executor(String param){
        System.out.println("这是xxl-job的第一个定时任务" + param + System.currentTimeMillis());
        XxlJobLogger.log("this is a myJobHandler!");
        return ReturnT.SUCCESS;
    }
}
