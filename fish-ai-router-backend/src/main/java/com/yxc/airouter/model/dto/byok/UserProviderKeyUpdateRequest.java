package com.yxc.airouter.model.dto.byok;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserProviderKeyUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 密钥 ID
     */
    private Long id;

    /**
     * 新的 API Key（可选）
     */
    private String apiKey;

    /**
     * 状态（可选）
     */
    private String status;
}
