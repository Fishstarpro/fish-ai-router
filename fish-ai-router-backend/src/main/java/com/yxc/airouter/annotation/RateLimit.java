package com.yxc.airouter.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 限流类型
     */
    LimitType type() default LimitType.API_KEY;
    
    /**
     * 限流数量（默认每秒 10 次）
     */
    int limit() default 10;
    
    /**
     * 时间窗口，默认 1
     */
    int window() default 1;
    
    /**
     * 时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 限流类型枚举
     */
    enum LimitType {
        API_KEY,  // 基于 API Key 限流
        IP        // 基于 IP 限流
    }
}
