package com.yxc.airouter.enums;

import lombok.Getter;

/**
 * 用户状态枚举
 *
 */
@Getter
public enum UserStatusEnum {

    ACTIVE("active", "正常"),
    DISABLED("disabled", "禁用");

    private final String value;
    private final String desc;

    UserStatusEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据值获取枚举
     */
    public static UserStatusEnum getByValue(String value) {
        for (UserStatusEnum statusEnum : values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}