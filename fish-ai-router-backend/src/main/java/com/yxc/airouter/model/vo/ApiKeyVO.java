package com.yxc.airouter.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ApiKeyVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * API Key值
     */
    private String keyValue;

    /**
     * Key名称/备注
     */
    private String keyName;

    /**
     * 状态
     */
    private String status;

    /**
     * 已使用Token总数
     */
    private Long totalTokens;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
