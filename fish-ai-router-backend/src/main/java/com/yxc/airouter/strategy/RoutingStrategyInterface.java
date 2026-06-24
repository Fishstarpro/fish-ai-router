package com.yxc.airouter.strategy;

import com.yxc.airouter.model.entity.Model;

import java.util.List;

/**
 * 路由策略接口
 */
public interface RoutingStrategyInterface {
    
    /**
     * 选择最优模型（从数据库查询并返回最符合策略的一条数据）
     */
    Model selectModel(String modelType, String requestedModel);
    
    /**
     * 获取 Fallback 模型列表（除了主选模型外的备选模型）
     */
    List<Model> getFallbackModels(String modelType, String requestedModel);
    
    /**
     * 获取策略类型
     */
    String getStrategyType();
}
