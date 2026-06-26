package com.yxc.airouter.aop;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.yxc.airouter.annotation.RateLimit;
import com.yxc.airouter.exception.BusinessException;
import com.yxc.airouter.exception.ErrorCode;
import com.yxc.airouter.service.RateLimitService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    @Resource
    private RateLimitService rateLimitService;

    @Around("@annotation(rateLimit)")
    public Object doRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        log.info("RateLimitAspect triggered, limit={}, type={}", rateLimit.limit(), rateLimit.type());

        //1.获取请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return joinPoint.proceed();
        }
        //2.根据限流类型获取限流key
        HttpServletRequest request = attributes.getRequest();

        String limitKey = getLimitKey(rateLimit, request);

        if (limitKey == null) {
            return joinPoint.proceed();
        }
        //3.执行方法返回许可
        //将Time转为Duration
        Duration duration = toDuration(rateLimit.window(), rateLimit.timeUnit());

        boolean allowed = rateLimitService.tryAcquire(limitKey, rateLimit.limit(), duration);

        if (!allowed) {
            log.warn("Rate limit exceeded, key: {}, {}/{} {}",
                    limitKey, rateLimit.window(), rateLimit.timeUnit());

            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "请求过于频繁,请稍后再试");
        }

        return joinPoint.proceed();
    }

    private Duration toDuration(int window, TimeUnit timeUnit) {
        return switch (timeUnit) {
            case NANOSECONDS -> Duration.ofNanos(window);
            case MICROSECONDS -> Duration.ofNanos(window * 1000);
            case MILLISECONDS -> Duration.ofMillis(window);
            case SECONDS -> Duration.ofSeconds(window);
            case MINUTES -> Duration.ofMinutes(window);
            case HOURS -> Duration.ofHours(window);
            case DAYS -> Duration.ofDays(window);
        };
    }

    private String getLimitKey(RateLimit rateLimit, HttpServletRequest request) {
        switch (rateLimit.type()) {
            case API_KEY:
                String authorization = request.getHeader("Authorization");

                if (authorization != null && authorization.startsWith("Bearer ")) {
                    return "rate_limit:api_key:" + authorization.substring(7);
                }

                return null;

            case IP:
                return "rate_limit:ip:" + JakartaServletUtil.getClientIP(request);

            default:
                return null;
        }

    }
}
