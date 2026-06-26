package com.yxc.airouter.service;

import java.time.Duration;

public interface RateLimitService {
    
    /**
     * 尝试获取限流许可
     */
    boolean tryAcquire(String key, int limit, Duration duration);
    
    /**
     * 获取当前可用许可数
     */
    long getAvailablePermits(String key);
    
    /**
     * 检查 API Key 是否被限流
     */
    boolean checkApiKeyRateLimit(String apiKey, int limit);
    
    /**
     * 检查 IP 是否被限流
     */
    boolean checkIpRateLimit(String ip, int limit);
}
