package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RBloomFilter rBloomFilter;

    /**
     * api回顾：
     * 获取目标方法参数：joinPoint.getArgs()
     * 获取目标方法所在类：joinPoint.getTarget().getClass()
     * 获取目标方法签名：(MethodSignature)joinPoint.getSignature()
     *
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        List<Object> args = Arrays.asList(joinPoint.getArgs());
        String pid = args.get(0).toString();
        if (!this.rBloomFilter.contains(pid)){
            return null;
        }

        // 获取方法签名对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取目标方法对象
        Method method = signature.getMethod();
        // 获取目标方法的返回值类型
        Class<?> returnType = method.getReturnType();
        // 获取目标方法上的指定注解对象
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);

        // 获取缓存前缀
        String prefix = gmallCache.prefix();
        // 获取方法参数
        String key = prefix + args;
        // 查询缓存，缓存中有直接返回
        String json = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseObject(json, returnType);
        }

        // 缓存中没有，加分布式锁
        String lock = gmallCache.lock() + args;
        RLock fairLock = this.redissonClient.getFairLock(lock);
        fairLock.lock();

        // 再去查询缓存，缓存中有直接返回
        String json2 = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json2)) {
            fairLock.unlock();
            return JSON.parseObject(json2, returnType);
        }

        // 执行目标方法
        Object result = joinPoint.proceed(joinPoint.getArgs());

        // 放入缓存，并释放锁
        int timeout = gmallCache.timeout() + new Random().nextInt(gmallCache.random());
        this.redisTemplate.opsForValue().set(key, JSON.toJSONString(result), timeout, TimeUnit.MINUTES);

        fairLock.unlock();

        return result;
    }
}
