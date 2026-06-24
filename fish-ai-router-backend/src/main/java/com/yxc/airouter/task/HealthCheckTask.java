package com.yxc.airouter.task;

import com.yxc.airouter.service.HealthCheckService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 健康检查定时任务
 */
@Component
@Slf4j
public class HealthCheckTask {
    
    @Resource
    private HealthCheckService healthCheckService;
    
    /**
     * 每 30 秒执行一次健康检查
     */
//    @Scheduled(fixedRate = 30000)
    public void executeHealthCheck() {
        log.info("=== 开始执行健康检查 ===");
        healthCheckService.checkAllProviders();
        log.info("=== 健康检查完成 ===");
    }
}
