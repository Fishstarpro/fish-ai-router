package com.yxc.airouter.service;

import com.yxc.airouter.model.dto.chat.ChatRequest;
import com.yxc.airouter.model.dto.chat.ChatResponse;
import reactor.core.publisher.Flux;

public interface ChatService {

    /**
     * 非流式聊天
     */
    ChatResponse chat(ChatRequest chatRequest, Long userId, Long apiKeyId);

    /**
     * 流式聊天
     */
    Flux<String> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId);
}
