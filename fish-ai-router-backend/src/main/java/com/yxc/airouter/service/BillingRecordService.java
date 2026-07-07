package com.yxc.airouter.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.yxc.airouter.model.entity.BillingRecord;

import java.math.BigDecimal;

/**
 * 消费账单服务
 *
 */
public interface BillingRecordService extends IService<BillingRecord> {

    /**
     * 获取用户的消费账单列表（分页）
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<BillingRecord> listUserBillingRecords(Long userId, int pageNum, int pageSize);

    /**
     * 获取用户的总消费金额
     *
     * @param userId 用户ID
     * @return 总消费金额
     */
    BigDecimal getUserTotalSpending(Long userId);

    /**
     * 获取用户的总充值金额
     *
     * @param userId 用户ID
     * @return 总充值金额
     */
    BigDecimal getUserTotalRecharge(Long userId);
}