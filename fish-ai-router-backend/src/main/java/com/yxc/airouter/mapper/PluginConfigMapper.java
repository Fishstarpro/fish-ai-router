/**
 * 插件配置 Mapper
 *
 */
package com.yxc.airouter.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yxc.airouter.model.entity.PluginConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PluginConfigMapper extends BaseMapper<PluginConfig> {
}