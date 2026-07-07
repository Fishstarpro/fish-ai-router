package com.yxc.airouter.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("billing_record")
public class BillingRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 关联的请求日志ID
     */
    @Column("requestLogId")
    private Long requestLogId;

    /**
     * 消费金额（元）
     */
    @Column("amount")
    private BigDecimal amount;

    /**
     * 消费前余额（元）
     */
    @Column("balanceBefore")
    private BigDecimal balanceBefore;

    /**
     * 消费后余额（元）
     */
    @Column("balanceAfter")
    private BigDecimal balanceAfter;

    /**
     * 消费说明
     */
    @Column("description")
    private String description;

    /**
     * 账单类型：api_call/recharge/refund
     */
    @Column("billingType")
    private String billingType;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;
}
