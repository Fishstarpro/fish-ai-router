package com.yxc.airouter.service;

import com.mybatisflex.core.service.IService;
import com.yxc.airouter.model.dto.byok.UserProviderKeyAddRequest;
import com.yxc.airouter.model.dto.byok.UserProviderKeyUpdateRequest;
import com.yxc.airouter.model.entity.UserProviderKey;
import com.yxc.airouter.model.vo.UserProviderKeyVO;

import java.util.List;

public interface UserProviderKeyService extends IService<UserProviderKey> {

    /**
     * 添加用户提供者密钥
     *
     * @param request 添加请求
     * @param userId  用户 ID
     * @return 是否成功
     */
    boolean addUserProviderKey(UserProviderKeyAddRequest request, Long userId);

    /**
     * 更新用户提供者密钥
     *
     * @param request 更新请求
     * @param userId  用户 ID
     * @return 是否成功
     */
    boolean updateUserProviderKey(UserProviderKeyUpdateRequest request, Long userId);

    /**
     * 删除用户提供者密钥
     *
     * @param id     密钥 ID
     * @param userId 用户 ID
     * @return 是否成功
     */
    boolean deleteUserProviderKey(Long id, Long userId);

    /**
     * 获取用户的所有提供者密钥列表
     *
     * @param userId 用户 ID
     * @return 密钥列表
     */
    List<UserProviderKeyVO> listUserProviderKeys(Long userId);

    /**
     * 获取用户在指定提供者的密钥（解密后）
     *
     * @param userId     用户 ID
     * @param providerId 提供者 ID
     * @return 解密后的 API Key，如果不存在返回 null
     */
    String getUserProviderApiKey(Long userId, Long providerId);

    /**
     * 检查用户是否配置了指定提供者的密钥
     *
     * @param userId     用户 ID
     * @param providerId 提供者 ID
     * @return 是否配置
     */
    boolean hasUserProviderKey(Long userId, Long providerId);
}
