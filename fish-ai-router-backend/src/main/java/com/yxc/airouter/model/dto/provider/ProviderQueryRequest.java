package com.yxc.airouter.model.dto.provider;

import com.yxc.airouter.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模型提供者查询请求
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProviderQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 提供者名称
     */
    private String providerName;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 状态：active/inactive/maintenance
     */
    private String status;

    /**
     * 健康状态：healthy/unhealthy/degraded/unknown
     */
    private String healthStatus;
}