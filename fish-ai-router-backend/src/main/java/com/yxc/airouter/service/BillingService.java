package com.yxc.airouter.service;

import com.yxc.airouter.model.entity.Model;

import java.math.BigDecimal;

public interface BillingService {
    /**
     * 计算本次请求的费用
     */
    BigDecimal calculateCost(Model model, int promptTokens, int completionTokens);
    
    /**
     * 根据模型ID计算费用
     */
    BigDecimal calculateCost(Long modelId, int promptTokens, int completionTokens);
    
    /**
     * 获取用户总费用
     */
    BigDecimal getUserTotalCost(Long userId);
    
    /**
     * 获取用户今日费用
     */
    BigDecimal getUserTodayCost(Long userId);
}
