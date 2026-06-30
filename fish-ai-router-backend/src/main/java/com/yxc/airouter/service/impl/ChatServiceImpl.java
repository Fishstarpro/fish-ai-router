package com.yxc.airouter.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yxc.airouter.exception.BusinessException;
import com.yxc.airouter.exception.ErrorCode;
import com.yxc.airouter.model.dto.chat.*;
import com.yxc.airouter.model.dto.log.RequestLogDTO;
import com.yxc.airouter.model.entity.Model;
import com.yxc.airouter.model.entity.ModelProvider;
import com.yxc.airouter.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

  /*  @Resource
    private ChatModel chatModel;*/

    @Resource
    private RequestLogService requestLogService;

    @Resource
    private RoutingService routingService;

    /**
     * 最大 Fallback 重试次数
     */
    private static final int MAX_FALLBACK_RETRIES = 3;
    @Autowired
    private ModelProviderService modelProviderService;
    @Autowired
    private ModelInvokeService modelInvokeService;

    /*@Override
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

    }*/

    @Override
    public ChatResponse chat(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent) {
        //1.确定路由策略,优先根据指定的策略,如果没有则根据指定的模型
        String strategyType = determineStategyType(chatRequest.getRoutingStrategy(), chatRequest.getModel());
        //2.根据策略获取模型
        Model selectedModel = routingService.selectModel(strategyType, "chat", chatRequest.getModel());

        if (selectedModel == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "没有可用的模型");
        }
        //3.使用带有fallback机制的调用
        List<Model> fallbackModels = routingService.getFallbackModels(strategyType, "chat", chatRequest.getModel());

        String traceId = IdUtil.simpleUUID();

        long startTime = System.currentTimeMillis();

        try {
            return invokeModelWithFallback(selectedModel, fallbackModels, chatRequest,
                    userId, apiKeyId, traceId, startTime, strategyType, false, clientIp, userAgent);
        } catch (Exception e) {
            log.error("所有模型调用失败", e);

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用模型失败: " + e.getMessage());
        }
    }

    private ChatResponse invokeModelWithFallback(Model selectedModel, List<Model> fallbackModels,
                                                 ChatRequest chatRequest, Long userId, Long apiKeyId,
                                                 String traceId, long startTime, String strategyType,
                                                 boolean b, String clientIp, String userAgent) {
        //1.调用主模型
        try {
            return callModel(selectedModel, chatRequest, userId, apiKeyId, traceId, startTime, strategyType, false, clientIp, userAgent);
        } catch (Exception e) {
            log.warn("模型 {} 调用失败，尝试 Fallback", selectedModel.getModelKey(), e);

            //2.有备用模型调用备用模型
            if (fallbackModels != null &&  !fallbackModels.isEmpty()) {
                int retries = Math.min(fallbackModels.size(), MAX_FALLBACK_RETRIES);

                for (int i = 0; i < retries; i++) {
                    Model fallbackModel = fallbackModels.get(i);

                    try {
                        log.info("尝试 Fallback 模型: {}", fallbackModel.getModelKey());

                        return callModel(fallbackModel, chatRequest, userId, apiKeyId, traceId, startTime, strategyType, true, clientIp, userAgent);
                    } catch (Exception fallbackException) {
                        log.warn("Fallback 模型 {} 调用失败", fallbackModel.getModelKey(), fallbackException);

                        if (i == retries - 1) {
                            throw fallbackException;
                        }
                    }
                }
            }

            throw e;
        }
    }

    private ChatResponse callModel(Model model, ChatRequest chatRequest, Long userId,
                                   Long apiKeyId, String traceId, long startTime,
                                   String strategyType, boolean isFallback, String clientIp, String userAgent) {
        //1.获取提供者
        ModelProvider provider = modelProviderService.getById(model.getProviderId());

        if (provider == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模型提供者不存在");
        }
        try {
            //2.调用模型
            org.springframework.ai.chat.model.ChatResponse aiResponse = modelInvokeService.invoke(
                    model, provider, chatRequest
            );
            //3.转换响应
            ChatResponse chatResponse = convertResponse(aiResponse, model.getModelKey());
            //4.记录调用日志
            long duration = System.currentTimeMillis() - startTime;

            requestLogService.logRequest(RequestLogDTO.builder()
                    .modelId(model.getId())
                    .userId(userId)
                    .traceId(traceId)
                    .apiKeyId(apiKeyId)
                    .requestModel(model.getModelKey())
                    .requestType("chat")
                    .source(apiKeyId != null? "api" : "web")
                    .promptTokens(chatResponse.getUsage().getPromptTokens())
                    .completionTokens(chatResponse.getUsage().getCompletionTokens())
                    .totalTokens(chatResponse.getUsage().getTotalTokens())
                    .duration((int) duration)
                    .status("success")
                    .routingStrategy(strategyType)
                    .isFallback(isFallback)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .build());

            return chatResponse;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            requestLogService.logRequest(RequestLogDTO.builder()
                    .modelId(model.getId())
                    .traceId(traceId)
                    .userId(userId)
                    .apiKeyId(apiKeyId)
                    .modelId(model.getId())
                    .requestModel(model.getModelKey())
                    .requestType("chat")
                    .source(apiKeyId != null ? "api" : "web")
                    .duration((int) duration)
                    .status("failed")
                    .errorMessage(e.getMessage())
                    .errorCode("MODEL_ERROR")
                    .routingStrategy(strategyType)
                    .isFallback(isFallback)
                    .clientIp(clientIp)
                    .userAgent(userAgent)
                    .build());

            throw e;
        }
    }

    private String determineStategyType(String routingStrategy, String model) {
        if (StrUtil.isNotBlank(routingStrategy)) {
            return routingStrategy;
        }

        if (StrUtil.isNotBlank(model)) {
            return "fixed";
        }

        return "auto";
    }


   /* @Override
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
    }*/

    /*@Override
    public Flux<String> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent) {
        try {
            //1.确定路由策略,优先根据指定的策略,如果没有则根据指定的模型
            String strategyType = determineStategyType(chatRequest.getRoutingStrategy(), chatRequest.getModel());
            //2.根据策略获取模型
            Model selectedModel = routingService.selectModel(strategyType, "chat", chatRequest.getModel());

            if (selectedModel == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "没有可用的模型");
            }
            //3.获取提供者
            ModelProvider provider = modelProviderService.getById(selectedModel.getProviderId());
            //4.流式调用
            long startTime = System.currentTimeMillis();

            final int[] promptTokens = {0};
            final int[] completionTokens = {0};

            // 深度思考状态追踪
            final boolean[] thinkingStarted = {false};
            final boolean[] thinkingEnded = {false};
            final boolean isReasoningEnabled = chatRequest.getEnableReasoning() != null && chatRequest.getEnableReasoning();

            Flux<StreamChunk> streamChunkFlux = modelInvokeService.invokeStreamChunk(selectedModel, provider, chatRequest);

            return streamChunkFlux.flatMap(chunk -> {
                //获取token信息
                if (chunk.getPromptTokens() != null && chunk.getPromptTokens() > 0) {
                    promptTokens[0] = chunk.getPromptTokens();
                }

                if (chunk.getCompletionTokens() != null && chunk.getCompletionTokens() > 0) {
                    completionTokens[0] = chunk.getCompletionTokens();
                }
                //处理深度思考内容
                if (isReasoningEnabled && chunk.hasReasoningContent()) {
                    String reasoningContent = chunk.getReasoningContent();

                    if (!thinkingStarted[0]) {
                        thinkingStarted[0] = true;

                        String thinkText = "[THINKING]" + escapeNewline(reasoningContent);

                        return Flux.just(thinkText + "\n\n");
                    }

                    if (!thinkingEnded[0]) {
                        return Flux.just(escapeNewline(reasoningContent) + "\n\n");
                    }
                }

                if (isReasoningEnabled && thinkingStarted[0] && !thinkingEnded[0] && !chunk.hasReasoningContent()) {
                    thinkingEnded[0] = true;

                    String endTag = "[/THINKING]" + "\n\n";

                    if (chunk.hasText())
                    return Flux.just(endTag + escapeNewline(chunk.getText()) + "\n\n");
                }
                //处理普通文本内容
                if (chunk.hasText()) {
                    return Flux.just(escapeNewline(chunk.getText()) + "\n\n");
                }

                return Flux.empty();
            }).doOnComplete(() -> {
                String  traceId = IdUtil.simpleUUID();
                // 流结束时记录日志
                long duration = System.currentTimeMillis() - startTime;
                int totalTokens = promptTokens[0] + completionTokens[0];
                requestLogService.logRequest(RequestLogDTO.builder()
                        .traceId(traceId)
                        .userId(userId)
                        .apiKeyId(apiKeyId)
                        .modelId(selectedModel.getId())
                        .requestModel(selectedModel.getModelKey())
                        .requestType("chat")
                        .source(apiKeyId != null ? "api" : "web")
                        .promptTokens(promptTokens[0])
                        .completionTokens(completionTokens[0])
                        .totalTokens(totalTokens)
                        .duration((int) duration)
                        .status("success")
                        .routingStrategy(strategyType)
                        .isFallback(false)
                        .clientIp(clientIp)
                        .userAgent(userAgent)
                        .build());
            }).doOnError(error -> {
                String  traceId = IdUtil.simpleUUID();
                // 流错误时记录日志
                log.error("流式调用模型失败", error);
                long duration = System.currentTimeMillis() - startTime;
                requestLogService.logRequest(RequestLogDTO.builder()
                        .traceId(traceId)
                        .userId(userId)
                        .apiKeyId(apiKeyId)
                        .modelId(selectedModel.getId())
                        .requestModel(selectedModel.getModelKey())
                        .requestType("chat")
                        .source(apiKeyId != null ? "api" : "web")
                        .duration((int) duration)
                        .status("failed")
                        .errorMessage(error.getMessage())
                        .errorCode("STREAM_ERROR")
                        .routingStrategy(strategyType)
                        .isFallback(false)
                        .clientIp(clientIp)
                        .userAgent(userAgent)
                        .build());
            });
        } catch (Exception e) {
            log.error("流式调用模型失败", e);
            return Flux.error(new BusinessException(ErrorCode.SYSTEM_ERROR, "流式调用模型失败: " + e.getMessage()));
        }
    }*/

    @Override
    public Flux<StreamResponse> chatStream(ChatRequest chatRequest, Long userId, Long apiKeyId, String clientIp, String userAgent) {
        try {
            //1.确定路由策略,优先根据指定的策略,如果没有则根据指定的模型
            String strategyType = determineStategyType(chatRequest.getRoutingStrategy(), chatRequest.getModel());
            //2.根据策略获取模型
            Model selectedModel = routingService.selectModel(strategyType, "chat", chatRequest.getModel());

            if (selectedModel == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "没有可用的模型");
            }
            //3.获取提供者
            ModelProvider provider = modelProviderService.getById(selectedModel.getProviderId());
            //4.流式调用
            long startTime = System.currentTimeMillis();

            final int[] promptTokens = {0};
            final int[] completionTokens = {0};
            final boolean[] isFirstChunk = {true};

            // 深度思考状态追踪
            final boolean[] thinkingStarted = {false};
            final boolean[] thinkingEnded = {false};
            final boolean isReasoningEnabled = chatRequest.getEnableReasoning() != null && chatRequest.getEnableReasoning();

            String traceId = IdUtil.simpleUUID();

            long created = System.currentTimeMillis() / 1000;

            Flux<StreamChunk> streamChunkFlux = modelInvokeService.invokeStreamChunk(selectedModel, provider, chatRequest);

            return streamChunkFlux.flatMap(chunk -> {
                //获取token信息
                if (chunk.getPromptTokens() != null && chunk.getPromptTokens() > 0) {
                    promptTokens[0] = chunk.getPromptTokens();
                }

                if (chunk.getCompletionTokens() != null && chunk.getCompletionTokens() > 0) {
                    completionTokens[0] = chunk.getCompletionTokens();
                }
                // 构建 Delta
                StreamResponse.Delta.DeltaBuilder deltaBuilder = StreamResponse.Delta.builder();
                // 第一个块包含 role
                if (isFirstChunk[0]) {
                    deltaBuilder.role("assistant");
                    isFirstChunk[0] = false;
                }

                // 处理普通文本内容
                if (chunk.hasText()) {
                    deltaBuilder.content(chunk.getText());
                }

                // 处理深度思考内容
                if (chunk.hasReasoningContent()) {
                    deltaBuilder.reasoningContent(chunk.getReasoningContent());
                }

                // 如果既没有文本也没有思考内容，跳过
                if (!chunk.hasText() && !chunk.hasReasoningContent()) {
                    return Flux.empty();
                }

                StreamResponse.Delta delta = deltaBuilder.build();

                StreamResponse.StreamChoice choice = StreamResponse.StreamChoice.builder()
                        .index(0)
                        .delta(delta)
                        .finishReason(null)
                        .build();

                return Flux.just(StreamResponse.builder()
                        .id(traceId)
                        .object("chat.completion.chunk")
                        .created(created)
                        .model(selectedModel.getModelKey())
                        .choices(List.of(choice))
                        .build());
            }).concatWith(Flux.defer(() -> {
                        StreamResponse.StreamChoice finishChoice = StreamResponse.StreamChoice.builder()
                                .index(0)
                                .delta(StreamResponse.Delta.builder().build())
                                .finishReason("stop")
                                .build();
                        return Flux.just(StreamResponse.builder()
                                .id(traceId)
                                .object("chat.completion.chunk")
                                .created(created)
                                .model(selectedModel.getModelKey())
                                .choices(List.of(finishChoice))
                                .build());
             })).doOnComplete(() -> {
                // 流结束时记录日志
                long duration = System.currentTimeMillis() - startTime;
                int totalTokens = promptTokens[0] + completionTokens[0];
                requestLogService.logRequest(RequestLogDTO.builder()
                        .traceId(traceId)
                        .userId(userId)
                        .apiKeyId(apiKeyId)
                        .modelId(selectedModel.getId())
                        .requestModel(selectedModel.getModelKey())
                        .requestType("chat")
                        .source(apiKeyId != null ? "api" : "web")
                        .promptTokens(promptTokens[0])
                        .completionTokens(completionTokens[0])
                        .totalTokens(totalTokens)
                        .duration((int) duration)
                        .status("success")
                        .routingStrategy(strategyType)
                        .isFallback(false)
                        .clientIp(clientIp)
                        .userAgent(userAgent)
                        .build());
            }).doOnError(error -> {
                // 流错误时记录日志
                log.error("流式调用模型失败", error);
                long duration = System.currentTimeMillis() - startTime;
                requestLogService.logRequest(RequestLogDTO.builder()
                        .traceId(traceId)
                        .userId(userId)
                        .apiKeyId(apiKeyId)
                        .modelId(selectedModel.getId())
                        .requestModel(selectedModel.getModelKey())
                        .requestType("chat")
                        .source(apiKeyId != null ? "api" : "web")
                        .duration((int) duration)
                        .status("failed")
                        .errorMessage(error.getMessage())
                        .errorCode("STREAM_ERROR")
                        .routingStrategy(strategyType)
                        .isFallback(false)
                        .clientIp(clientIp)
                        .userAgent(userAgent)
                        .build());
            });
        } catch (Exception e) {
            log.error("流式调用模型失败", e);
            return Flux.error(new BusinessException(ErrorCode.SYSTEM_ERROR, "流式调用模型失败: " + e.getMessage()));
        }
    }

    private String escapeNewline(String text) {
        return text.replaceAll("\n", "\\n");
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
