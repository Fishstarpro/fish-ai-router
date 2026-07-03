package com.yxc.airouter.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yxc.airouter.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 原子递增用户已使用Token数（SET used_tokens = used_tokens + ?）
     */
    @Update("UPDATE user SET usedTokens = COALESCE(usedTokens, 0) + #{tokens} WHERE id = #{userId}")
    int incrementUsedTokens(@Param("userId") Long userId, @Param("tokens") int tokens);
}
