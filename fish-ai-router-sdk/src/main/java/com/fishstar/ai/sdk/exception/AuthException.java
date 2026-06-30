package com.fishstar.ai.sdk.exception;

/**
 * 认证异常 - API Key 无效或过期
 */
public class AuthException extends YuAIException {

    public AuthException(String message) {
        super(401, message);
    }

    public AuthException(String message, Throwable cause) {
        super(401, message, cause);
    }
}