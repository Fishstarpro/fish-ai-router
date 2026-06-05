package com.yxc.airouter.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
import com.yxc.airouter.exception.BusinessException;
import com.yxc.airouter.exception.ErrorCode;
import com.yxc.airouter.model.dto.chat.ChatMessage;
import com.yxc.airouter.model.dto.chat.ChatRequest;
import com.yxc.airouter.model.dto.chat.ChatResponse;
import com.yxc.airouter.service.ChatService;
import com.yxc.airouter.service.RequestLogService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private ChatModel chatModel;

    @Resource
    private RequestLogService requestLogService;

    @Override
    public ChatResponse chat(ChatRequest chatRequest, Long userId, Long apiKeyId) {
        long startTime = System.currentTimeMillis();

        String modelName = chatRequest.getModel();

        try {
            //1.构建prompt
            Prompt prompt = buildPrompt(chatRequest);
            //2.通过chatclient发送请求
            ChatClient chatClient = ChatClient.builder(chatModel).build();

            org.springframework.ai.chat.model.ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
            //3.将响应提取封装到自己构建的chatresponse
            ChatResponse response = convertResponse(chatResponse, modelName);
            //4.记录调用日志
            long duration = System.currentTimeMillis() - startTime;

            requestLogService.logRequest(userId, apiKeyId, modelName, response.getUsage().getPromptTokens(),
                    response.getUsage().getCompletionTokens(),
                    response.getUsage().getTotalTokens(),
                    (int)duration,
                    "success",
                    null);

            return response;
        } catch (Exception e) {
            log.error("调用模型失败", e);

            long duration = System.currentTimeMillis() - startTime;

            requestLogService.logRequest(userId, apiKeyId, modelName, 0, 0, 0,
                    (int)duration,
                    "failed",
                    e.getMessage());

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用模型失败: " + e.getMessage());
        }

    }

    @Override
    public Flux<String> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId) {
        String modelName = chatRequest.getModel();

        long startTime = System.currentTimeMillis();
        //token计数器
        final int[] promptTokens = {0};

        final int[] completionTokens = {0};
        try {
            //1.构建prompt
            Prompt prompt = buildPrompt(chatRequest);
            //2.通过chatclient发送请求得到响应
            ChatClient chatClient = ChatClient.builder(chatModel).build();

            Flux<org.springframework.ai.chat.model.ChatResponse> flux = chatClient.prompt(prompt)
                    .stream()
                    .chatResponse();
            //3.解析响应
            return flux.flatMap(response -> {
                //获取token信息,一般是最后一个chunk中有
                if (response.getMetadata().getUsage() != null) {
                    Integer promptToken = response.getMetadata().getUsage().getPromptTokens();

                    Integer completionToken = response.getMetadata().getUsage().getCompletionTokens();

                    if (promptToken != null && promptToken > 0) {
                        promptTokens[0] = promptToken;
                    }

                    if (completionToken != null && completionToken > 0) {
                        completionTokens[0] = completionToken;
                    }
                }

                //获取文本信息
                //最后一个chunk只有token信息,没有内容,直接返回
                if (ObjUtil.isEmpty(response.getResult()) || ObjUtil.isEmpty(response.getResult().getOutput())) {
                    return Flux.empty();
                }
                //将文本中的换行符"\n"转义为"\\n",避免与SSE格式冲突
                String text = response.getResult().getOutput().getText();

                String newText = text.replace("\n", "\\n");

                return Flux.just(newText + "\n\n");
            }).doOnComplete(() -> {
                //4.通过钩子函数记录日志
                long duration = System.currentTimeMillis() - startTime;

                int totalTokens = promptTokens[0] + completionTokens[0];

                requestLogService.logRequest(userId, apiKeyId, modelName,
                        promptTokens[0], completionTokens[0], totalTokens, (int)duration, "success", null);
            }).doOnError(error -> {
                log.error("流式调用模型失败: " + error.getMessage());

                long duration = System.currentTimeMillis() - startTime;

                requestLogService.logRequest(userId, apiKeyId, modelName,
                        0, 0, 0, (int)duration, "failed", error.getMessage());
            });
        } catch (Exception e) {
            log.error("流式调用模型失败: " + e.getMessage());

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流式调用模型失败: " + e.getMessage());
        }
    }

    /**
     * 构建 Prompt
     */
    private Prompt buildPrompt(ChatRequest chatRequest) {
        List<Message> messages = chatRequest.getMessages().stream()
                .map(message -> switch (message.getRole()) {
                    case "system" -> new SystemMessage(message.getContent());
                    case "assistant" -> new AssistantMessage(message.getContent());
                    default -> new UserMessage(message.getContent());
                }).collect(Collectors.toList());

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(chatRequest.getModel())
                .streamUsage(true);

        if (chatRequest.getTemperature() != null) {
            optionsBuilder.temperature(chatRequest.getTemperature());
        }

        if (chatRequest.getMaxTokens() != null) {
            optionsBuilder.maxTokens(chatRequest.getMaxTokens());
        }

        return new Prompt(messages, optionsBuilder.build());
    }

    /**
     * 转换响应格式
     */
    private ChatResponse convertResponse(org.springframework.ai.chat.model.ChatResponse aiResponse, String modelName) {
        ChatResponse.Usage usage = ChatResponse.Usage.builder()
                .promptTokens(aiResponse.getMetadata().getUsage().getPromptTokens() != null ? aiResponse.getMetadata().getUsage().getPromptTokens() : 0)
                .completionTokens(aiResponse.getMetadata().getUsage().getCompletionTokens() != null ? aiResponse.getMetadata().getUsage().getCompletionTokens() : 0)
                .totalTokens(aiResponse.getMetadata().getUsage().getTotalTokens() != null ? aiResponse.getMetadata().getUsage().getTotalTokens() : 0)
                .build();

        ChatResponse.Choice choice = ChatResponse.Choice.builder()
                .index(0)
                .message(new ChatMessage("assistant", aiResponse.getResult().getOutput().getText()))
                .finishReason(aiResponse.getResult().getMetadata().getFinishReason())
                .build();

        return ChatResponse.builder()
                .id(IdUtil.simpleUUID())
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .choices(List.of(choice))
                .usage(usage)
                .build();
    }
}
