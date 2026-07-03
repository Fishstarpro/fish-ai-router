package com.yxc.airouter.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yxc.airouter.mapper.RequestLogMapper;
import com.yxc.airouter.model.dto.log.RequestLogDTO;
import com.yxc.airouter.model.dto.log.RequestLogQueryRequest;
import com.yxc.airouter.model.entity.RequestLog;
import com.yxc.airouter.service.ApiKeyService;
import com.yxc.airouter.service.RequestLogService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                .cost(requestLogDTO.getCost())
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

    @Override
    public Long countUserRequests(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId);
        return requestLogMapper.selectCountByQuery(queryWrapper);
    }

    @Override
    public Long countUserSuccessRequests(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .eq("status", "success");
        return requestLogMapper.selectCountByQuery(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> getUserDailyStats(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = LocalDateTime.of(currentDate, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(currentDate, LocalTime.MAX);

            List<RequestLog> logs = requestLogMapper.selectListByQuery(
                    QueryWrapper.create()
                            .eq("userId", userId)
                            .ge("createTime", dayStart)
                            .le("createTime", dayEnd)
            );

            long totalTokens = 0;
            long requestCount = 0;
            long successCount = 0;
            BigDecimal totalCost = BigDecimal.ZERO;

            for (RequestLog log : logs) {
                requestCount++;
                if ("success".equals(log.getStatus())) {
                    successCount++;
                    if (log.getTotalTokens() != null) {
                        totalTokens += log.getTotalTokens();
                    }
                    if (log.getCost() != null) {
                        totalCost = totalCost.add(log.getCost());
                    }
                }
            }

            Map<String, Object> dayStats = new HashMap<>();
            dayStats.put("date", currentDate.toString());
            dayStats.put("totalTokens", totalTokens);
            dayStats.put("requestCount", requestCount);
            dayStats.put("successCount", successCount);
            dayStats.put("totalCost", totalCost);
            result.add(dayStats);

            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    @Override
    public Page<RequestLog> pageByQuery(RequestLogQueryRequest queryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();

        // 用户ID条件
        if (queryRequest.getUserId() != null) {
            queryWrapper.eq("userId", queryRequest.getUserId());
        }

        // 模型标识
        if (StrUtil.isNotBlank(queryRequest.getRequestModel())) {
            queryWrapper.like("requestModel", queryRequest.getRequestModel());
        }

        // 请求类型
        if (StrUtil.isNotBlank(queryRequest.getRequestType())) {
            queryWrapper.eq("requestType", queryRequest.getRequestType());
        }

        // 调用来源
        if (StrUtil.isNotBlank(queryRequest.getSource())) {
            queryWrapper.eq("source", queryRequest.getSource());
        }

        // 状态
        if (StrUtil.isNotBlank(queryRequest.getStatus())) {
            queryWrapper.eq("status", queryRequest.getStatus());
        }

        // 日期范围
        if (StrUtil.isNotBlank(queryRequest.getStartDate())) {
            LocalDateTime startTime = LocalDateTime.of(LocalDate.parse(queryRequest.getStartDate()), LocalTime.MIN);
            queryWrapper.ge("createTime", startTime);
        }
        if (StrUtil.isNotBlank(queryRequest.getEndDate())) {
            LocalDateTime endTime = LocalDateTime.of(LocalDate.parse(queryRequest.getEndDate()), LocalTime.MAX);
            queryWrapper.le("createTime", endTime);
        }

        // 按创建时间倒序
        queryWrapper.orderBy("createTime", false);

        long pageNum = queryRequest.getPageNum();
        long pageSize = queryRequest.getPageSize();
        return requestLogMapper.paginate(Page.of(pageNum, pageSize), queryWrapper);
    }

    @Override
    public RequestLog getById(Long id) {
        if (id == null) {
            return null;
        }
        return requestLogMapper.selectOneById(id);
    }

    @Override
    public Page<RequestLog> pageUserLogs(Long userId, long pageNum, long pageSize) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .orderBy("createTime", false);

        return requestLogMapper.paginate(Page.of(pageNum, pageSize), queryWrapper);
    }
}
