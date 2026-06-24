package com.yxc.airouter.service.impl;

import com.yxc.airouter.model.entity.Model;
import com.yxc.airouter.service.RoutingService;
import com.yxc.airouter.strategy.RoutingStrategyInterface;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoutingServiceImpl implements RoutingService {

    @Resource
    List<RoutingStrategyInterface> routingStrategyList;

    @Override
    public Model selectModel(String strategyType, String modelType, String requestedModel) {
        //1.从不同的策略中获取可用的模型
        RoutingStrategyInterface routingStrategy = routingStrategyList.stream()
                .filter(r -> r.getStrategyType().equals(strategyType))
                .findFirst()
                .orElse(null);

        return routingStrategy.selectModel(modelType, requestedModel);
    }

    @Override
    public List<Model> getFallbackModels(String strategyType, String modelType, String requestedModel) {
        //1.从不同的策略中获取可用的备用模型
        RoutingStrategyInterface routingStrategy = routingStrategyList.stream()
                .filter(r -> r.getStrategyType().equals(strategyType))
                .findFirst()
                .orElse(null);

        return routingStrategy.getFallbackModels(modelType, requestedModel);
    }
}
