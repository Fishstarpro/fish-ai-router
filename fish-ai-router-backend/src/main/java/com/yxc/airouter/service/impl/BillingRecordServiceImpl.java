package com.yxc.airouter.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yxc.airouter.mapper.BillingRecordMapper;
import com.yxc.airouter.model.entity.BillingRecord;
import com.yxc.airouter.service.BillingRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 消费账单服务实现
 *
 */
@Service
@Slf4j
public class BillingRecordServiceImpl extends ServiceImpl<BillingRecordMapper, BillingRecord> implements BillingRecordService {

    @Override
    public Page<BillingRecord> listUserBillingRecords(Long userId, int pageNum, int pageSize) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where("userId = ?", userId)
                .orderBy("createTime", false);

        return page(Page.of(pageNum, pageSize), queryWrapper);
    }

    @Override
    public BigDecimal getUserTotalSpending(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("SUM(amount)")
                .where("userId = ?", userId)
                .and("billingType = ?", "api_call");

        BigDecimal total = mapper.selectObjectByQueryAs(queryWrapper, BigDecimal.class);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getUserTotalRecharge(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("SUM(amount)")
                .where("userId = ?", userId)
                .and("billingType = ?", "recharge");

        BigDecimal total = mapper.selectObjectByQueryAs(queryWrapper, BigDecimal.class);
        return total != null ? total : BigDecimal.ZERO;
    }
}