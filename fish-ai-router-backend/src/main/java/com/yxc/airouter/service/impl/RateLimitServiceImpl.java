package com.yxc.airouter.service.impl;

import com.yxc.airouter.service.RateLimitService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimitServiceImpl implements RateLimitService {

    @Resource
    private RedissonClient redissonClient;

    /**
     * API Key 限流前缀
     */
    private static final String RATE_LIMIT_API_KEY_PREFIX = "rate_limit:api_key:";

    /**
     * IP 限流前缀
     */
    private static final String RATE_LIMIT_IP_PREFIX = "rate_limit:ip:";

    /**
     * 记录本实例已经初始化过的限流 key，避免重复 reset
     */
    private final Set<String> initializedKeys = ConcurrentHashMap.newKeySet();

    @Override
    public boolean tryAcquire(String key, int limit, Duration duration) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // 首次使用该 key 时先删后设，确保 @RateLimit 注解参数变更后新配置生效
        if (initializedKeys.add(key)) {
            rateLimiter.delete();
            rateLimiter.trySetRate(RateType.OVERALL, limit, duration);
        }

        boolean acquired = rateLimiter.tryAcquire(1);

        if (!acquired) {
            log.warn("Rate limit exceeded for key: {}", key);
        }

        return acquired;
    }

    @Override
    public long getAvailablePermits(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        return rateLimiter.availablePermits();
    }

    @Override
    public boolean checkApiKeyRateLimit(String apiKey, int limit) {
        String key = RATE_LIMIT_API_KEY_PREFIX + apiKey;
        return tryAcquire(key, limit, Duration.ofSeconds(1));
    }

    @Override
    public boolean checkIpRateLimit(String ip, int limit) {
        String key = RATE_LIMIT_IP_PREFIX + ip;
        return tryAcquire(key, limit, Duration.ofSeconds(1));
    }
}
