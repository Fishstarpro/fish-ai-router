package com.yxc.airouter.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.mybatisflex.core.query.QueryWrapper;
import com.yxc.airouter.enums.HealthStatusEnum;
import com.yxc.airouter.enums.ProviderStatusEnum;
import com.yxc.airouter.mapper.RequestLogMapper;
import com.yxc.airouter.model.entity.Model;
import com.yxc.airouter.model.entity.ModelProvider;
import com.yxc.airouter.service.HealthCheckService;
import com.yxc.airouter.service.ModelProviderService;
import com.yxc.airouter.service.ModelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 健康检查服务实现
 */
@Service
@Slf4j
public class HealthCheckServiceImpl implements HealthCheckService {
    
    @Resource
    private ModelProviderService modelProviderService;
    
    @Resource
    private ModelService modelService;
    
    @Resource
    private RequestLogMapper requestLogMapper;
    
    // 存储最近的健康检查结果（滑动窗口）
    private final ConcurrentHashMap<Long, List<Boolean>> healthHistoryMap = new ConcurrentHashMap<>();
    
    // 健康检查历史记录最大数量
    private static final int MAX_HISTORY_SIZE = 100;
    
    // 统计请求日志的时间范围（小时）
    private static final int STATS_HOURS = 24;

    /**
     * 成本权重（用于计算综合得分）
     */
    private static final double COST_WEIGHT = 0.3;

    /**
     * 延迟权重（用于计算综合得分）
     */
    private static final double LATENCY_WEIGHT = 0.3;

    /**
     * 成功率权重（用于计算综合得分）
     */
    private static final double SUCCESS_RATE_WEIGHT = 0.2;

    /**
     * 优先级权重（用于计算综合得分）
     */
    private static final double PRIORITY_WEIGHT = 0.2;

    @Override
    public boolean checkProviderHealth(ModelProvider provider) {
        if (provider == null) {
            return false;
        }

        try {
            long startTime = System.currentTimeMillis();
            //1.发送测试请求
            boolean isHealth = sendHealthCheckRequest(provider);
            //2.记录延迟
            long endTime = System.currentTimeMillis();

            int latency = (int)(endTime - startTime);
            //3.记录健康检查结果到healthHistoryMap
            recordHealthyCheckResult(provider.getId(),isHealth);
            //4.计算测试成功率
            BigDecimal successRate = calculateSuccessRate(provider.getId());
            //5.计算健康状态
            String healthyStatus = calculateHealthStatus(successRate);
            //6.更新provider的信息
            modelProviderService.updateHealthStatus(provider.getId(), healthyStatus, latency, successRate);

            log.debug("提供者{} 健康检查完成: 健康={}, 成功率= {}%, 延迟={}ms",
                    provider.getDisplayName(), healthyStatus, successRate, latency);

            return isHealth;
        } catch (Exception e) {
            log.error("提供者{} 健康检查失败", provider.getDisplayName(), e);
            //7.记录健康检查结果
            recordHealthyCheckResult(provider.getId(), false);
            //8.计算测试成功率
            BigDecimal successRate = calculateSuccessRate(provider.getId());
            //9.更新provider信息
            modelProviderService.updateHealthStatus(provider.getId(), HealthStatusEnum.UNHEALTHY.getValue(), 0, successRate);

            return false;
        }
    }

    /**
     * 根据成功率确定健康状态
     */
    private String calculateHealthStatus(BigDecimal successRate) {
        double rate = successRate.doubleValue();

        if (rate >= 80.0) {
            return HealthStatusEnum.HEALTHY.getValue();
        } else if (rate >= 50.0) {
            return HealthStatusEnum.DEGRADED.getValue();
        } else {
            return HealthStatusEnum.UNHEALTHY.getValue();
        }
    }

    /**
     * 计算成功率
     */
    private BigDecimal calculateSuccessRate(Long providerId) {
        List<Boolean> history = healthHistoryMap.get(providerId);

        if (CollUtil.isEmpty(history)) {
            return BigDecimal.valueOf(100.00);
        }

        long successCount = history.stream().filter(result -> result).count();

        double rate = (double) successCount / history.size() * 100;

        return BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private void recordHealthyCheckResult(Long id, boolean isHealth) {
        List<Boolean> healthHistory = healthHistoryMap.computeIfAbsent(id, key -> new ArrayList<>());

        healthHistory.add(isHealth);

        if (healthHistory.size() > MAX_HISTORY_SIZE) {
            healthHistory.remove(0);
        }
    }

    /**
     * 发送健康检查请求
     */
    private boolean sendHealthCheckRequest(ModelProvider provider) {
        try {
            String baseUrl = provider.getBaseUrl();
            String apiKey = provider.getApiKey();

            // 构造一个简单的测试请求（检查模型列表接口）
            String url = baseUrl + "/models";

            HttpResponse response = HttpRequest.get(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(10000)
                    .execute();

            // 检查响应状态码
            return response.getStatus() == 200 || response.getStatus() == 401;
        } catch (Exception e) {
            log.warn("提供者 {} 健康检查请求失败: {}", provider.getDisplayName(), e.getMessage());
            return false;
        }
    }

    @Override
    public void checkAllProviders() {
        // 获取所有启用的提供者
        List<ModelProvider> providers = modelProviderService.list();
        if (CollUtil.isEmpty(providers)) {
            log.warn("没有找到任何模型提供者");
            return;
        }

        log.info("开始健康检查，共 {} 个提供者", providers.size());

        // 并行检查所有提供者
        providers.parallelStream()
                .filter(provider -> ProviderStatusEnum.ACTIVE.getValue().equals(provider.getStatus()))
                .forEach(this::checkProviderHealth);

        // 从请求日志同步模型指标
        syncModelMetricsFromRequestLog();

        log.info("健康检查完成");
    }

    @Override
    public int measureLatency(ModelProvider provider) {
        try {
            long startTime = System.currentTimeMillis();
            sendHealthCheckRequest(provider);
            long endTime = System.currentTimeMillis();
            return (int) (endTime - startTime);
        } catch (Exception e) {
            log.error("测量提供者 {} 延迟失败", provider.getDisplayName(), e);
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public void syncModelMetricsFromRequestLog() {
        log.info("开始从请求日志同步模型指标");
        //1.获取所有活跃的模型
        List<Model> activeModels = modelService.getActiveModels();

        if (CollUtil.isEmpty(activeModels)) {
            log.error("没有活跃的模型");

            return;
        }
        //2.使用聚合函数一次性获取所有模型的指标
        //计算在当前时间之前创建的模型
        LocalDateTime startTime = LocalDateTime.now().minusHours(STATS_HOURS);

        List<ModelStats> modelStatsList = queryModelStatsFromDb(startTime);
        //将list转换为map,方便查找
        Map<Long, ModelStats> modelStatsMap = modelStatsList.stream()
                .collect(Collectors.toMap(modelStats -> modelStats.modelId, modelStats -> modelStats, (a, b) -> a));
        //3.计算归一化参数
        NormalizationParams normalizationParams = calculateNormalizationParams(activeModels, modelStatsMap);
        //4.更新模型的指标
        for (Model model : activeModels) {
            ModelStats modelStats = modelStatsMap.getOrDefault(model.getId(), new ModelStats());

            BigDecimal score = calculateScore(model, modelStats, normalizationParams);

            String healthStatus = calculateHealthStatus(modelStats.successRate);

            modelService.updateModelMetrics(
                    model.getId(),
                    healthStatus,
                    modelStats.avgLatency,
                    modelStats.successRate,
                    score
            );

            log.debug("模型{} 指标更新: 延迟={}ms, 成功率={}%, 得分={}",
                    model.getModelKey(),
                    modelStats.avgLatency,
                    modelStats.successRate,
                    score);
        }
        //5.更新提供者的指标
        syncProviderMetricsFromModelStats(activeModels, modelStatsMap);

        log.info("模型指标同步完成");
    }

    private void syncProviderMetricsFromModelStats(List<Model> activeModels, Map<Long, ModelStats> modelStatsMap) {
        //1.根据提供者分组
        Map<Long, List<Model>> providerModels = activeModels.stream()
                .collect(Collectors.groupingBy(Model::getProviderId));
        //2.遍历每个分组更新提供者的指标
        for (Map.Entry<Long, List<Model>> entry : providerModels.entrySet()) {
            Long providerId = entry.getKey();
            List<Model> providerModelList = entry.getValue();

            // 计算该提供者所有模型的平均延迟和成功率
            int totalLatency = 0;
            int latencyCount = 0;
            double totalSuccessRate = 0;
            int successRateCount = 0;

            for (Model model : providerModelList) {
                ModelStats stats = modelStatsMap.get(model.getId());
                if (stats != null && stats.avgLatency > 0) {
                    totalLatency += stats.avgLatency;
                    latencyCount++;
                }
                if (stats != null && stats.totalRequests > 0) {
                    totalSuccessRate += stats.successRate.doubleValue();
                    successRateCount++;
                }
            }

            int avgLatency = latencyCount > 0 ? totalLatency / latencyCount : 0;
            BigDecimal avgSuccessRate = successRateCount > 0
                    ? BigDecimal.valueOf(totalSuccessRate / successRateCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(100);

            // 根据成功率确定健康状态
            String healthStatus = calculateHealthStatus(avgSuccessRate);

            modelProviderService.updateHealthStatus(providerId, healthStatus, avgLatency, avgSuccessRate);

            log.debug("提供者 {} 指标更新: 延迟={}ms, 成功率={}%, 健康状态={}",
                    providerId, avgLatency, avgSuccessRate, healthStatus);
        }
    }

    /**
     * 计算模型的综合得分（越低越好）
     */
    private BigDecimal calculateScore(Model model, ModelStats stats, NormalizationParams params) {
        // 1. 成本得分（归一化到 0-1）
        BigDecimal modelCost = model.getInputPrice().add(model.getOutputPrice());
        double costScore = normalize(modelCost, params.minCost, params.maxCost);

        // 2. 延迟得分（归一化到 0-1）
        int latency = stats.avgLatency > 0 ? stats.avgLatency : 5000; // 默认5秒
        double latencyScore = normalize(latency, params.minLatency, params.maxLatency);

        // 3. 成功率得分（归一化到 0-1，成功率越高得分越低）
        double successRateScore = 1.0 - (stats.successRate.doubleValue() / 100.0);

        // 4. 优先级得分（归一化到 0-1，优先级越高得分越低）
        double priorityScore = 1.0 - normalize(model.getPriority(), params.minPriority, params.maxPriority);

        // 综合得分
        double score = costScore * COST_WEIGHT +
                latencyScore * LATENCY_WEIGHT +
                successRateScore * SUCCESS_RATE_WEIGHT +
                priorityScore * PRIORITY_WEIGHT;

        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 归一化数值到 0-1 范围
     */
    private double normalize(BigDecimal value, BigDecimal min, BigDecimal max) {
        BigDecimal range = max.subtract(min);
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return value.subtract(min).divide(range, 4, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 归一化数值到 0-1 范围
     */
    private double normalize(int value, int min, int max) {
        int range = max - min;
        if (range == 0) {
            return 0.0;
        }
        return (double) (value - min) / range;
    }

    private NormalizationParams calculateNormalizationParams(List<Model> models, Map<Long, ModelStats> statsMap) {
        NormalizationParams params = new NormalizationParams();

        // 成本范围
        params.minCost = models.stream()
                .map(m -> m.getInputPrice().add(m.getOutputPrice()))
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        params.maxCost = models.stream()
                .map(m -> m.getInputPrice().add(m.getOutputPrice()))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);

        // 延迟范围
        params.minLatency = statsMap.values().stream()
                .mapToInt(s -> s.avgLatency)
                .filter(l -> l > 0)
                .min()
                .orElse(0);
        params.maxLatency = statsMap.values().stream()
                .mapToInt(s -> s.avgLatency)
                .max()
                .orElse(10000);

        // 优先级范围
        params.minPriority = models.stream()
                .mapToInt(Model::getPriority)
                .min()
                .orElse(0);
        params.maxPriority = models.stream()
                .mapToInt(Model::getPriority)
                .max()
                .orElse(100);

        return params;
    }

    private List<ModelStats> queryModelStatsFromDb(LocalDateTime startTime) {
        // 使用 SQL 聚合函数一次性查询所有模型的统计数据
        // SELECT modelId,
        //        AVG(CASE WHEN status = 'success' THEN duration ELSE NULL END) as avgLatency,
        //        COUNT(*) as totalRequests,
        //        SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 100.0 / COUNT(*) as successRate
        // FROM request_log
        // WHERE createTime >= ? AND modelId IS NOT NULL
        // GROUP BY modelId

        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("modelId",
                        "AVG(CASE WHEN status = 'success' THEN duration ELSE NULL END) as avgLatency",
                        "COUNT(*) as totalRequests",
                        "IFNULL(SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 100) as successRate")
                .from("request_log")
                .ge("createTime", startTime)
                .isNotNull("modelId")
                .groupBy("modelId");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) (List<?>) requestLogMapper.selectListByQueryAs(queryWrapper, Map.class);

        List<ModelStats> statsList = new ArrayList<>();
        for (Map<String, Object> row : results) {
            ModelStats stats = new ModelStats();
            stats.modelId = row.get("modelId") != null ? ((Number) row.get("modelId")).longValue() : null;
            stats.avgLatency = row.get("avgLatency") != null ? ((Number) row.get("avgLatency")).intValue() : 0;
            stats.totalRequests = row.get("totalRequests") != null ? ((Number) row.get("totalRequests")).intValue() : 0;
            stats.successRate = row.get("successRate") != null
                    ? BigDecimal.valueOf(((Number) row.get("successRate")).doubleValue()).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(100);
            statsList.add(stats);
        }

        return statsList;
    }

    /**
     * 模型统计数据内部类
     */
    private static class ModelStats {
        Long modelId;
        int avgLatency = 0;
        BigDecimal successRate = BigDecimal.valueOf(100);
        int totalRequests = 0;
    }

    /**
     * 归一化参数内部类
     */
    private static class NormalizationParams {
        BigDecimal minCost = BigDecimal.ZERO;
        BigDecimal maxCost = BigDecimal.ONE;
        int minLatency = 0;
        int maxLatency = 10000;
        int minPriority = 0;
        int maxPriority = 100;
    }
}
