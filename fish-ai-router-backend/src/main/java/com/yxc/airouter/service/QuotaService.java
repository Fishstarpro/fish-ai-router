package com.yxc.airouter.service;

public interface QuotaService {
    /**
     * 检查用户是否有足够的配额
     */
    boolean checkQuota(Long userId);
    
    /**
     * 扣减用户的Token使用量
     */
    boolean deductTokens(Long userId, int tokens);
    
    /**
     * 获取用户剩余配额
     */
    long getRemainingQuota(Long userId);
}
