package com.yxc.airouter.service;

import com.mybatisflex.core.service.IService;
import com.yxc.airouter.model.entity.ApiKey;
import com.yxc.airouter.model.entity.User;

import java.util.List;

public interface ApiKeyService extends IService<ApiKey> {

    /**
     * 创建 API Key
     */
    ApiKey createApiKey(String keyName, User loginUser);

    /**
     * 获取用户的 API Key 列表
     */
    List<ApiKey> listUserApiKeys(Long userId);

    /**
     * 撤销 API Key
     */
    boolean revokeApiKey(Long id, Long userId);

    /**
     * 根据 Key 值查询 API Key
     */
    ApiKey getByKeyValue(String keyValue);

    /**
     * 更新 API Key 的使用统计
     */
    void updateUsageStats(Long apiKeyId, Integer tokens);
}
