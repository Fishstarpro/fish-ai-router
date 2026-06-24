package com.yxc.airouter.service.impl;

import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.yxc.airouter.mapper.RequestLogMapper;
import com.yxc.airouter.model.dto.log.RequestLogDTO;
import com.yxc.airouter.model.entity.RequestLog;
import com.yxc.airouter.service.ApiKeyService;
import com.yxc.airouter.service.RequestLogService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RequestLogServiceImpl implements RequestLogService {

    @Resource
    private RequestLogMapper requestLogMapper;

    @Resource
    private ApiKeyService apiKeyService;

    @Override
    @Async
    public void logRequest(Long userId, Long apiKeyId, String modelName,
                           Integer promptTokens, Integer completionTokens, Integer totalTokens,
                           Integer duration, String status, String errorMessage) {
        // 创建请求日志
        RequestLog log = RequestLog.builder()
                .userId(userId)
                .apiKeyId(apiKeyId)
                .modelName(modelName)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .duration(duration)
                .status(status)
                .errorMessage(errorMessage)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 保存日志
        requestLogMapper.insert(log);

        // 更新 API Key 的使用统计（仅成功的请求）
        if ("success".equals(status) && apiKeyId != null && totalTokens != null && totalTokens > 0) {
            apiKeyService.updateUsageStats(apiKeyId, totalTokens);
        }
    }

    @Override
    public void logRequest(RequestLogDTO requestLogDTO) {
        if (requestLogDTO == null) {
            return;
        }

        // 如果没有 traceId，生成一个
        if (requestLogDTO.getTraceId() == null) {
            requestLogDTO.setTraceId(IdUtil.simpleUUID());
        }

        // 创建请求日志
        RequestLog log = RequestLog.builder()
                .traceId(requestLogDTO.getTraceId())
                .userId(requestLogDTO.getUserId())
                .apiKeyId(requestLogDTO.getApiKeyId())
                .modelId(requestLogDTO.getModelId())
                .requestModel(requestLogDTO.getRequestModel())
                .modelName(requestLogDTO.getRequestModel())  // 兼容字段
                .requestType(requestLogDTO.getRequestType() != null ? requestLogDTO.getRequestType() : "chat")
                .source(requestLogDTO.getSource() != null ? requestLogDTO.getSource() : "web")
                .promptTokens(requestLogDTO.getPromptTokens() != null ? requestLogDTO.getPromptTokens() : 0)
                .completionTokens(requestLogDTO.getCompletionTokens() != null ? requestLogDTO.getCompletionTokens() : 0)
                .totalTokens(requestLogDTO.getTotalTokens() != null ? requestLogDTO.getTotalTokens() : 0)
                .duration(requestLogDTO.getDuration() != null ? requestLogDTO.getDuration() : 0)
                .status(requestLogDTO.getStatus())
                .errorMessage(requestLogDTO.getErrorMessage())
                .errorCode(requestLogDTO.getErrorCode())
                .routingStrategy(requestLogDTO.getRoutingStrategy())
                .isFallback(requestLogDTO.getIsFallback() != null && requestLogDTO.getIsFallback() ? 1 : 0)
                .clientIp(requestLogDTO.getClientIp())
                .userAgent(requestLogDTO.getUserAgent())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 保存日志
        requestLogMapper.insert(log);

        // 更新 API Key 的使用统计（仅成功的请求且有 apiKeyId）
        if ("success".equals(requestLogDTO.getStatus()) &&
                requestLogDTO.getApiKeyId() != null &&
                requestLogDTO.getTotalTokens() != null &&
                requestLogDTO.getTotalTokens() > 0) {
            apiKeyService.updateUsageStats(requestLogDTO.getApiKeyId(), requestLogDTO.getTotalTokens());
        }
    }

    @Override
    public List<RequestLog> listUserLogs(Long userId, Integer limit) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .orderBy("createTime", false)
                .limit(limit != null ? limit : 100);

        return requestLogMapper.selectListByQuery(queryWrapper);
    }

    @Override
    public Long countUserTokens(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("SUM(totalTokens)")
                .eq("userId", userId)
                .eq("status", "success");

        Long total = requestLogMapper.selectObjectByQueryAs(queryWrapper, Long.class);
        return total != null ? total : 0L;
    }
}
