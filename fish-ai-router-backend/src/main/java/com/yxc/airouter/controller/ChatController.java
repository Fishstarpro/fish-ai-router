package com.yxc.airouter.controller;

import com.yxc.airouter.exception.BusinessException;
import com.yxc.airouter.exception.ErrorCode;
import com.yxc.airouter.model.dto.chat.ChatRequest;
import com.yxc.airouter.model.entity.ApiKey;
import com.yxc.airouter.service.ApiKeyService;
import com.yxc.airouter.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/chat")
@Slf4j
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private ApiKeyService apiKeyService;

    @PostMapping(value = "/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    @Operation(summary = "Chat Completions")
    public Object chatCompletions(@RequestBody ChatRequest chatRequest,
                                @RequestHeader(value = "Authorization", required = false) String authorization,
                                HttpServletRequest request) {
        //1.验证api key
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "缺少或无效的authorization");
        }

        String apiKeyValue = authorization.substring(7);

        ApiKey apiKey = apiKeyService.getByKeyValue(apiKeyValue);

        if (apiKey == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "apiKey无效或已失效");
        }
        //2.验证chatRequest
        if (chatRequest.getMessages() == null || chatRequest.getMessages().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "messages不能为空");
        }
        //设置默认模型
        if (chatRequest.getModel() == null || chatRequest.getModel().isEmpty()) {
            chatRequest.setModel("qwen-plus");
        }
        //3.判断是否为流式请求
        if (chatRequest.getStream() != null && chatRequest.getStream()) {
            return chatService.chatStream(chatRequest, apiKey.getUserId(), apiKey.getId());
        } else {
            return chatService.chat(chatRequest, apiKey.getUserId(), apiKey.getId());
        }
    }


}
