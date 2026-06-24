package com.yxc.airouter.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.yxc.airouter.enums.HealthStatusEnum;
import com.yxc.airouter.enums.ModelStatusEnum;
import com.yxc.airouter.enums.RoutingStrategyTypeEnum;
import com.yxc.airouter.model.entity.Model;
import com.yxc.airouter.service.ModelService;
import com.yxc.airouter.strategy.RoutingStrategyInterface;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 成本优先路由策略
 */
@Component
public class CostFirstRoutingStrategy implements RoutingStrategyInterface {

    @Resource
    private ModelService  modelService;

    @Override
    public Model selectModel(String modelType, String requestedModel) {
        //1.构建查询条件
        QueryWrapper queryWrapper = buildQueryWrapper(modelType);

        queryWrapper.orderBy("(inputPrice + outputPrice)", true);

        queryWrapper.limit(1);
        //2.返回第一个
        return modelService.getOne(queryWrapper);
    }

    private QueryWrapper buildQueryWrapper(String modelType) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("status", ModelStatusEnum.ACTIVE.getValue())
                .in("healthStatus", HealthStatusEnum.HEALTHY.getValue(),
                        HealthStatusEnum.DEGRADED.getValue(),
                        HealthStatusEnum.UNKNOWN.getValue());

        if (StrUtil.isNotBlank(modelType)) {
            queryWrapper.eq("modelType", modelType);
        }

        return queryWrapper;
    }

    @Override
    public List<Model> getFallbackModels(String modelType, String requestedModel) {
        //1.构建查询条件
        QueryWrapper queryWrapper = buildQueryWrapper(modelType);

        queryWrapper.orderBy("(inputPrice + outputPrice)", true);
        //2.跳过第一个
        List<Model> modelList = modelService.list(queryWrapper);

        return modelList.stream()
                .skip(1)
                .collect(Collectors.toList());
    }

    @Override
    public String getStrategyType() {
        return RoutingStrategyTypeEnum.COST_FIRST.getValue();
    }
}
