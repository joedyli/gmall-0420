package com.atguigu.gmall.index.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Thread thread;

    public Boolean tryLock(String lockName, String uuid, Long expireTime){
        String script = "if(redis.call('exists', KEYS[1])==0 or redis.call('hexists', KEYS[1], ARGV[1])==1) " +
                "then " +
                "   redis.call('hincrby', KEYS[1], ARGV[1], 1); " +
                "   redis.call('expire', KEYS[1], ARGV[2]); " +
                "   return 1; " +
                "else " +
                "   return 0; " +
                "end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expireTime.toString());
        if (!flag){
            try {
                Thread.sleep(30);
                tryLock(lockName, uuid, expireTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.renewExpire(lockName, uuid, expireTime);
        return true;
    }

    public void unlock(String lockName, String uuid){
        String script = "if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) " +
                "then " +
                "   return nil " +
                "elseif(redis.call('hincrby', KEYS[1], ARGV[1], -1) > 0) " +
                "then " +
                "   return 0 " +
                "else " +
                "   redis.call('del', KEYS[1]) " +
                "   return 1 " +
                "end";
        Long flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid);
        thread.interrupt();
        if (flag == null) {
            throw new RuntimeException("对应的lock锁不存或者这个锁不属于您！");
        }
    }

    private void renewExpire(String lockName, String uuid, Long expireTime){
        String script = "if(redis.call('hexists', KEYS[1], ARGV[1]) == 1) then redis.call('expire', KEYS[1], ARGV[2]) return 1 else return 0 end";
        thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(expireTime * 1000 * 2 / 3);
                    // 自动续期
                    this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expireTime.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "");
        thread.start();
    }
}
