package com.atguigu.gmall.index.aspect;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    /**
     * 缓存key的前缀
     * @return
     */
    String prefix() default "";

    /**
     * 缓存的超时时间：单位是分钟
     * @return
     */
    int timeout() default 5;

    /**
     * 为了避免缓存雪崩，给缓存时间添加随机值：单位分钟
     * @return
     */
    int random() default 5;

    /**
     * 分布锁的前缀
     * @return
     */
    String lock() default "lock:";
}
