package com.yxc.airouter.service;

import com.yxc.airouter.model.dto.chat.ChatRequest;
import com.yxc.airouter.model.dto.chat.ChatResponse;
import com.yxc.airouter.model.dto.chat.StreamResponse;
import reactor.core.publisher.Flux;

public interface ChatService {

    /**
     * 非流式聊天
     */
//    ChatResponse chat(ChatRequest chatRequest, Long userId, Long apiKeyId);

    ChatResponse chat(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent);


    /**
     * 流式聊天
     */
//    Flux<String> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId);

    //Flux<String> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent);

    Flux<StreamResponse> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent);
}
