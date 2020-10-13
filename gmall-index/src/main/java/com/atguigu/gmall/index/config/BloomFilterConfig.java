package com.atguigu.gmall.index.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {

    @Autowired
    private RedissonClient redissonClient;

    @Bean
    public RBloomFilter rBloomFilter(){
        RBloomFilter<String> bloomfilter = this.redissonClient.getBloomFilter("bloomfilter");
        bloomfilter.tryInit(50l, 0.03);
        return bloomfilter;
    }
}
