package com.atguigu.gmall.index;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Deserializers;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.hash.PrimitiveSink;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.List;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private RBloomFilter<String> bloomFilter;

//    @PostConstruct
//    public void init(){
//        this.redisTemplate.setKeySerializer(new StringRedisSerializer());
//        this.redisTemplate.setValueSerializer(new StringRedisSerializer());
//    }

    @Test
    void contextLoads() {
        this.redisTemplate.opsForValue().set("user2", "liuyan");
        System.out.println(this.redisTemplate.opsForValue().get("user2"));
    }

    @Test
    public void testBloom(){
        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 10, 0.3);
        bloomFilter.put("1");
        bloomFilter.put("2");
        bloomFilter.put("3");
        bloomFilter.put("4");
        bloomFilter.put("5");
        System.out.println(bloomFilter.mightContain("1"));
        System.out.println(bloomFilter.mightContain("3"));
        System.out.println(bloomFilter.mightContain("5"));
        System.out.println(bloomFilter.mightContain("6"));
        System.out.println(bloomFilter.mightContain("7"));
        System.out.println(bloomFilter.mightContain("8"));
        System.out.println(bloomFilter.mightContain("9"));
        System.out.println(bloomFilter.mightContain("10"));
        System.out.println(bloomFilter.mightContain("11"));
        System.out.println(bloomFilter.mightContain("12"));
        System.out.println(bloomFilter.mightContain("13"));
        System.out.println(bloomFilter.mightContain("14"));
        System.out.println(bloomFilter.mightContain("15"));
        System.out.println(bloomFilter.mightContain("16"));
        System.out.println(bloomFilter.mightContain("17"));
        System.out.println(bloomFilter.mightContain("18"));
    }

    @Test
    public void testRedissonBloom(){
        RBloomFilter<String> bloom = this.redissonClient.getBloomFilter("bloom");
        bloom.tryInit(10l, 0.03);
        bloom.add("1");
        bloom.add("2");
        bloom.add("3");
        bloom.add("4");
        bloom.add("5");
        System.out.println(bloom.contains("1"));
        System.out.println(bloom.contains("3"));
        System.out.println(bloom.contains("5"));
        System.out.println(bloom.contains("6"));
        System.out.println(bloom.contains("7"));
        System.out.println(bloom.contains("8"));
        System.out.println(bloom.contains("9"));
        System.out.println(bloom.contains("10"));
        System.out.println(bloom.contains("11"));
        System.out.println(bloom.contains("12"));
        System.out.println(bloom.contains("13"));
        System.out.println(bloom.contains("14"));
        System.out.println(bloom.contains("15"));
        System.out.println(bloom.contains("16"));
    }

    @Test
    public void testBloomRedisson1(){
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesByPid(0l);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        if (!CollectionUtils.isEmpty(categoryEntities)){
            categoryEntities.forEach(categoryEntity -> {
                bloomFilter.add(categoryEntity.getId().toString());
            });
        }
    }


}
