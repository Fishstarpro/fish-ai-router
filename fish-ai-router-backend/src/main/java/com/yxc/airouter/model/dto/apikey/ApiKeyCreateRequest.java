package com.yxc.airouter.model.dto.apikey;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ApiKeyCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Key名称/备注
     */
    private String keyName;
}
