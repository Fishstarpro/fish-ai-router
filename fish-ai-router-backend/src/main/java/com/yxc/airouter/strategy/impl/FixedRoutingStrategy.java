package com.yxc.airouter.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.yxc.airouter.model.enums.HealthStatusEnum;
import com.yxc.airouter.model.enums.ModelStatusEnum;
import com.yxc.airouter.model.enums.RoutingStrategyTypeEnum;
import com.yxc.airouter.model.entity.Model;
import com.yxc.airouter.service.ModelService;
import com.yxc.airouter.strategy.RoutingStrategyInterface;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 固定模型路由策略
 * 使用用户指定的模型
 */
@Component
public class FixedRoutingStrategy implements RoutingStrategyInterface {

    @Resource
    private ModelService modelService;

    @Override
    public Model selectModel(String modelType, String requestedModel) {
        // 固定模型策略必须指定模型
        if (StrUtil.isBlank(requestedModel)) {
            return null;
        }

        // 查找指定的模型
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("modelKey", requestedModel)
                .eq("status", ModelStatusEnum.ACTIVE.getValue())
                .in("healthStatus", HealthStatusEnum.HEALTHY.getValue(),
                        HealthStatusEnum.DEGRADED.getValue(), HealthStatusEnum.UNKNOWN.getValue());

        // 找不到指定模型直接返回 null，由上层报错
        return modelService.getOne(queryWrapper);
    }

    @Override
    public List<Model> getFallbackModels(String modelType, String requestedModel) {
        QueryWrapper queryWrapper = buildBaseQueryWrapper(modelType);
        queryWrapper.orderBy("priority", false);

        List<Model> models = modelService.list(queryWrapper);

        // 如果指定了模型，排除指定模型
        return models.stream()
                .filter(m -> !requestedModel.equals(m.getModelKey()))
                .collect(Collectors.toList());

    }

    @Override
    public String getStrategyType() {
        return RoutingStrategyTypeEnum.FIXED.getValue();
    }

    /**
     * 构建基础查询条件：状态为启用且健康状态为健康或降级或未知
     */
    private QueryWrapper buildBaseQueryWrapper(String modelType) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("status", ModelStatusEnum.ACTIVE.getValue())
                .in("healthStatus", HealthStatusEnum.HEALTHY.getValue(),
                        HealthStatusEnum.DEGRADED.getValue(), HealthStatusEnum.UNKNOWN.getValue());

        if (StrUtil.isNotBlank(modelType)) {
            queryWrapper.eq("modelType", modelType);
        }

        return queryWrapper;
    }
}
