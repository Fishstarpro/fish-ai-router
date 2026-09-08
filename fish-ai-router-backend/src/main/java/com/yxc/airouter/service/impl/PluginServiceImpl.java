package com.yxc.airouter.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yxc.airouter.exception.BusinessException;
import com.yxc.airouter.exception.ErrorCode;
import com.yxc.airouter.mapper.PluginConfigMapper;
import com.yxc.airouter.model.dto.plugin.PluginExecuteRequest;
import com.yxc.airouter.model.dto.plugin.PluginUpdateRequest;
import com.yxc.airouter.model.entity.PluginConfig;
import com.yxc.airouter.model.enums.PluginStatusEnum;
import com.yxc.airouter.model.vo.PluginConfigVO;
import com.yxc.airouter.model.vo.PluginExecuteVO;
import com.yxc.airouter.plugin.Plugin;
import com.yxc.airouter.plugin.PluginContext;
import com.yxc.airouter.plugin.PluginManager;
import com.yxc.airouter.plugin.PluginResult;
import com.yxc.airouter.service.PluginService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PluginServiceImpl extends ServiceImpl<PluginConfigMapper, PluginConfig> implements PluginService {

    @Resource
    private PluginManager pluginManager;

    @Resource
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        initPlugins();
    }

    @Override
    public List<PluginConfigVO> listAllPlugins() {
        List<PluginConfig> pluginConfigs = list(QueryWrapper.create()
                .orderBy("priority", false));
        return pluginConfigs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PluginConfigVO> listEnabledPlugins() {
        List<PluginConfig> pluginConfigs = list(QueryWrapper.create()
                .eq("status", PluginStatusEnum.ACTIVE.getValue())
                .orderBy("priority", false));
        return pluginConfigs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public PluginConfigVO getPluginByKey(String pluginKey) {
        PluginConfig pluginConfig = getOne(QueryWrapper.create()
                .eq("pluginKey", pluginKey));
        if (pluginConfig == null) {
            return null;
        }
        return convertToVO(pluginConfig);
    }

    @Override
    public boolean updatePlugin(PluginUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "插件 ID 不能为空");
        }

        PluginConfig pluginConfig = getById(request.getId());
        if (pluginConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "插件不存在");
        }

        // 更新字段
        if (request.getPluginName() != null) {
            pluginConfig.setPluginName(request.getPluginName());
        }
        if (request.getDescription() != null) {
            pluginConfig.setDescription(request.getDescription());
        }
        if (request.getConfig() != null) {
            pluginConfig.setConfig(request.getConfig());
        }
        if (request.getStatus() != null) {
            pluginConfig.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            pluginConfig.setPriority(request.getPriority());
        }

        boolean result = updateById(pluginConfig);
        if (result) {
            // 更新插件管理器中的配置
            pluginManager.updatePluginConfig(pluginConfig);
        }
        return result;
    }

    @Override
    public boolean enablePlugin(String pluginKey) {
        PluginConfig pluginConfig = getOne(QueryWrapper.create()
                .eq("pluginKey", pluginKey));
        if (pluginConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "插件不存在");
        }

        pluginConfig.setStatus(PluginStatusEnum.ACTIVE.getValue());
        boolean result = updateById(pluginConfig);
        if (result) {
            pluginManager.updatePluginConfig(pluginConfig);
            log.info("启用插件: {}", pluginKey);
        }
        return result;
    }

    @Override
    public boolean disablePlugin(String pluginKey) {
        PluginConfig pluginConfig = getOne(QueryWrapper.create()
                .eq("pluginKey", pluginKey));
        if (pluginConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "插件不存在");
        }

        pluginConfig.setStatus(PluginStatusEnum.INACTIVE.getValue());
        boolean result = updateById(pluginConfig);
        if (result) {
            pluginManager.updatePluginConfig(pluginConfig);
            log.info("禁用插件: {}", pluginKey);
        }
        return result;
    }

    @Override
    public PluginExecuteVO executePlugin(PluginExecuteRequest request, Long userId) {
        //1.构建plugincontext
        String pluginKey = request.getPluginKey();

        if (pluginKey == null || pluginKey.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "插件标识不能为空");
        }

        PluginContext pluginContext = PluginContext.builder()
                .userId(userId)
                .input(request.getInput())
                .fileUrl(request.getFileUrl())
                .fileBytes(request.getFileBytes())
                .fileType(request.getFileType())
                .params(request.getParams())
                .build();
        //2.通过pluginmanager调用插件
        PluginResult pluginResult = pluginManager.executePlugin(pluginKey, pluginContext);
        //3.构建结果
        return PluginExecuteVO.builder()
                .success(pluginResult.isSuccess())
                .pluginKey(pluginKey)
                .content(pluginResult.getContent())
                .errorMessage(pluginResult.getErrorMessage())
                .duration(pluginResult.getDuration())
                .data(pluginResult.getData())
                .build();
    }

    @Override
    public void initPlugins() {
        //1.获取数据库中的pluginconfig
        log.info("初始化插件系统");

        List<PluginConfig> pluginConfigs = list();
        //2.获取所有注册的plugin bean
        Map<String, Plugin> plugins = applicationContext.getBeansOfType(Plugin.class);
        //3.plugin初始化对应的config
        for (Plugin plugin : plugins.values()) {
            String pluginKey = plugin.getPluginKey();

            PluginConfig config = pluginConfigs.stream()
                    .filter(pluginConfig -> pluginConfig.getPluginKey().equals(pluginKey))
                    .findFirst()
                    .orElse(null);

            if (config != null) {
                pluginManager.registerPlugin(plugin, config);
            } else {
                log.warn("插件 {} 在数据库中没有配置",  pluginKey);
            }
        }

        log.info("插件系统初始化完成, 共加载 {} 个插件", pluginManager.getPluginCount());
    }

    @Override
    public void reloadPlugin(String pluginKey) {
        //1.获取plugin对应的config
        PluginConfig pluginConfig = getOne(QueryWrapper.create()
                .eq("pluginKey", pluginKey));

        if (pluginConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "插件配置不存在");
        }
        //2.获取plugin
        Map<String, Plugin> plugins = applicationContext.getBeansOfType(Plugin.class);

        Plugin plugin = plugins.values().stream()
                .filter(p -> p.getPluginKey().equals(pluginKey))
                .findFirst()
                .orElse(null);

        if (plugin != null) {
            //3.通过pluginManager重新注册plugin
            pluginManager.registerPlugin(plugin, pluginConfig);
        } else {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "插件不存在");
        }
    }

    /**
     * 转换为 VO
     */
    private PluginConfigVO convertToVO(PluginConfig pluginConfig) {
        PluginConfigVO vo = new PluginConfigVO();
        BeanUtil.copyProperties(pluginConfig, vo);
        return vo;
    }
}
