package com.yxc.airouter.model.enums;

import lombok.Getter;

/**
 * 健康状态枚举
 *
 */
@Getter
public enum HealthStatusEnum {

    HEALTHY("healthy", "健康"),
    UNHEALTHY("unhealthy", "不健康"),
    DEGRADED("degraded", "降级"),
    UNKNOWN("unknown", "未知");

    private final String value;
    private final String text;

    HealthStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     */
    public static HealthStatusEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (HealthStatusEnum anEnum : HealthStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}