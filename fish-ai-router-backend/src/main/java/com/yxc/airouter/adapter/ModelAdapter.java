package com.yxc.airouter.adapter;

import com.yxc.airouter.model.dto.chat.ChatRequest;
import com.yxc.airouter.model.dto.chat.StreamChunk;
import com.yxc.airouter.model.entity.Model;
import com.yxc.airouter.model.entity.ModelProvider;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * 模型适配器接口
 */
public interface ModelAdapter {
    
    /**
     * 同步调用模型
     */
    ChatResponse invoke(Model model, ModelProvider provider, ChatRequest chatRequest);
    
    /**
     * 流式调用模型（返回原始响应）
     */
    Flux<ChatResponse> invokeStream(Model model, ModelProvider provider, ChatRequest chatRequest);
    
    /**
     * 流式调用模型（返回统一格式的响应块）
     */
    Flux<StreamChunk> invokeStreamChunk(Model model, ModelProvider provider, ChatRequest chatRequest);
    
    /**
     * 判断是否支持该提供者
     */
    boolean supports(String providerName);
}
