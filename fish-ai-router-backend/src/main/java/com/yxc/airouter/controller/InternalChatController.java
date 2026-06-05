package com.yxc.airouter.controller;

import com.yxc.airouter.annotation.AuthCheck;
import com.yxc.airouter.common.ResultUtils;
import com.yxc.airouter.constant.UserConstant;
import com.yxc.airouter.exception.BusinessException;
import com.yxc.airouter.exception.ErrorCode;
import com.yxc.airouter.model.dto.chat.ChatRequest;
import com.yxc.airouter.model.dto.chat.ChatResponse;
import com.yxc.airouter.model.entity.ApiKey;
import com.yxc.airouter.model.entity.User;
import com.yxc.airouter.service.ApiKeyService;
import com.yxc.airouter.service.ChatService;
import com.yxc.airouter.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/chat")
@Slf4j
public class InternalChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private ApiKeyService apiKeyService;

    @Resource
    private UserService userService;

    @PostMapping(value = "/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "内部聊天接口")
    public Object chatCompletions(@RequestBody ChatRequest chatRequest,
                                @RequestParam Long apiKeyId,
                                HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        //1.验证api key
        ApiKey apiKey = apiKeyService.getById(apiKeyId);

        if (apiKey == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "apiKey无效或已失效");
        }

        if (apiKey.getUserId() != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权使用该apiKey");
        }

        if (!apiKey.getStatus().equals("active")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "apikey已失效");
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
            return chatService.chatStream(chatRequest, loginUser.getId(), apiKey.getId());
        } else {
            ChatResponse response = chatService.chat(chatRequest, loginUser.getId(), apiKey.getId());

            return ResultUtils.success(response);
        }
    }


}
