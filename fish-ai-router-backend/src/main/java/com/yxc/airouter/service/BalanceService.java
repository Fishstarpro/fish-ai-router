package com.yxc.airouter.service;

import com.mybatisflex.core.service.IService;
import com.yxc.airouter.model.entity.User;

import java.math.BigDecimal;

public interface BalanceService extends IService<User> {
    
    /**
     * 检查余额是否充足
     */
    boolean checkBalance(Long userId, BigDecimal amount);
    
    /**
     * 扣减余额
     */
    boolean deductBalance(Long userId, BigDecimal amount, Long requestLogId, String description);
    
    /**
     * 增加余额
     */
    boolean addBalance(Long userId, BigDecimal amount, String description);
    
    /**
     * 获取用户余额
     */
    BigDecimal getUserBalance(Long userId);
    
    /**
     * 更新用户余额
     */
    boolean updateBalance(User user);
}
