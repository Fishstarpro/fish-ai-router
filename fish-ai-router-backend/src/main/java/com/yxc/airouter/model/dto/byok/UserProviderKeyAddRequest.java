package com.yxc.airouter.model.dto.byok;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserProviderKeyAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 提供者 ID
     */
    private Long providerId;

    /**
     * API Key
     */
    private String apiKey;
}
