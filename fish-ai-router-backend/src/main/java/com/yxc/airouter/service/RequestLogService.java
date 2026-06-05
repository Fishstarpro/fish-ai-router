package com.yxc.airouter.service;

import com.yxc.airouter.model.entity.RequestLog;

import java.util.List;

public interface RequestLogService {

    /**
     * 记录请求日志
     */
    void logRequest(Long userId, Long apiKeyId, String modelName,
                    Integer promptTokens, Integer completionTokens, Integer totalTokens,
                    Integer duration, String status, String errorMessage);

    /**
     * 查询用户的请求日志
     */
    List<RequestLog> listUserLogs(Long userId, Integer limit);

    /**
     * 统计用户的 Token 消耗
     */
    Long countUserTokens(Long userId);
}
