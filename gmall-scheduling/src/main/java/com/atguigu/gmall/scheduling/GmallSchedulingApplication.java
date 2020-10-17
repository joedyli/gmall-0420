package com.atguigu.gmall.scheduling;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.atguigu.gmall.scheduling.mapper")
public class GmallSchedulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallSchedulingApplication.class, args);
    }

}
