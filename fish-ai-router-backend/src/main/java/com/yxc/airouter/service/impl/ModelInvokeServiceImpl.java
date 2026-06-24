package com.yxc.airouter.service.impl;

import com.yxc.airouter.adapter.ModelAdapter;
import com.yxc.airouter.adapter.ModelAdapterFactory;
import com.yxc.airouter.model.dto.chat.ChatRequest;
import com.yxc.airouter.model.dto.chat.StreamChunk;
import com.yxc.airouter.model.entity.Model;
import com.yxc.airouter.model.entity.ModelProvider;
import com.yxc.airouter.service.ModelInvokeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 模型调用服务实现
 */
@Service
@Slf4j
public class ModelInvokeServiceImpl implements ModelInvokeService {
    
    @Resource
    private ModelAdapterFactory adapterFactory;

    @Override
    public ChatResponse invoke(Model model, ModelProvider provider, ChatRequest chatRequest) {
        log.info("调用模型: provider={}, model={}", provider.getProviderName(), model.getModelKey());
        
        // 根据提供者获取对应的适配器
        ModelAdapter adapter = adapterFactory.getAdapter(provider.getProviderName());
        
        // 使用适配器调用模型
        return adapter.invoke(model, provider, chatRequest);
    }

    @Override
    public Flux<ChatResponse> invokeStream(Model model, ModelProvider provider, ChatRequest chatRequest) {
        log.info("流式调用模型: provider={}, model={}", provider.getProviderName(), model.getModelKey());
        
        // 根据提供者获取对应的适配器
        ModelAdapter adapter = adapterFactory.getAdapter(provider.getProviderName());
        
        // 使用适配器流式调用模型
        return adapter.invokeStream(model, provider, chatRequest);
    }

    @Override
    public Flux<StreamChunk> invokeStreamChunk(Model model, ModelProvider provider, ChatRequest chatRequest) {
        log.info("流式调用模型(统一格式): provider={}, model={}", provider.getProviderName(), model.getModelKey());
        
        // 根据提供者获取对应的适配器
        ModelAdapter adapter = adapterFactory.getAdapter(provider.getProviderName());
        
        // 使用适配器流式调用模型，返回统一格式的响应块
        return adapter.invokeStreamChunk(model, provider, chatRequest);
    }
}
