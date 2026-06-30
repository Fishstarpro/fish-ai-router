package com.fishstar.ai.sdk.exception;

/**
 * Yu AI SDK 基础异常类
 */
public class YuAIException extends RuntimeException {

    private final int code;

    public YuAIException(String message) {
        super(message);
        this.code = -1;
    }

    public YuAIException(int code, String message) {
        super(message);
        this.code = code;
    }

    public YuAIException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
    }

    public YuAIException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}