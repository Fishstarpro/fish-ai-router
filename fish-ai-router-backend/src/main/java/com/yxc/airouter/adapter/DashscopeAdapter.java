package com.yxc.airouter.adapter;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.yxc.airouter.exception.BusinessException;
import com.yxc.airouter.exception.ErrorCode;
import com.yxc.airouter.model.dto.chat.ChatMessage;
import com.yxc.airouter.model.dto.chat.ChatRequest;
import com.yxc.airouter.model.dto.chat.StreamChunk;
import com.yxc.airouter.model.entity.Model;
import com.yxc.airouter.model.entity.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DashscopeAdapter implements ModelAdapter{
    /**
     * 适配器支持的模型类型
     */
    private static final Set<String> SUPPORTED_MODEL_TYPES = Set.of("qwen", "dashscope", "tongyi", "aliyun");
    @Override
    public ChatResponse invoke(Model model, ModelProvider provider, ChatRequest chatRequest) {
        try {
            //1.创建请求模型
            DashScopeChatModel chatModel = createChatModel(provider, model, chatRequest);
            //2.创建prompt
            Prompt prompt = buildPrompt(chatRequest);
            //3.返回响应
            return chatModel.call(prompt);
        } catch (Exception e) {
            log.error("Dashscope适配器调用模型 {} 失败", model.getModelKey(), e);

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用模型失败: " + e.getMessage());
        }
    }

    private Prompt buildPrompt(ChatRequest chatRequest) {
        List<Message> messages = chatRequest.getMessages().stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());

        return new Prompt(messages);
    }

    /**
     * 转换消息类型
     * @param chatMessage
     * @return
     */
    private Message convertMessage(ChatMessage chatMessage) {
        String role =  chatMessage.getRole();

        if (role == "system") {
            return new SystemMessage(chatMessage.getContent());
        } else if (role == "assistant") {
            return new AssistantMessage(chatMessage.getContent());
        } else {
            return new UserMessage(chatMessage.getContent());
        }
    }

    private DashScopeChatModel createChatModel(ModelProvider provider, Model model, ChatRequest chatRequest) {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(provider.getApiKey())
                .build();
        //构建选项
        DashScopeChatOptions options = buildOptions(model, chatRequest);

        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(options)
                .build();
    }

    private DashScopeChatOptions buildOptions(Model model, ChatRequest chatRequest) {
        DashScopeChatOptions options = new DashScopeChatOptions();

        options.setModel(model.getModelKey());

        if (chatRequest.getTemperature() != null) {
            options.setTemperature(chatRequest.getTemperature());
        }

        if (chatRequest.getEnableReasoning() != null && chatRequest.getEnableReasoning()
        && model.getSupportReasoning() != null && model.getSupportReasoning() == 1) {
            options.setEnableThinking(true);
        }

        return options;
    }

    @Override
    public Flux<ChatResponse> invokeStream(Model model, ModelProvider provider, ChatRequest chatRequest) {
        try {
            //1.创建请求模型
            DashScopeChatModel chatModel = createChatModel(provider, model, chatRequest);
            //2.创建prompt
            Prompt prompt = buildPrompt(chatRequest);
            //3.返回响应
            return chatModel.stream(prompt);
        } catch (Exception e) {
            log.error("Dashscope适配器流式调用模型 {} 失败", model.getModelKey(), e);

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流式调用模型失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<StreamChunk> invokeStreamChunk(Model model, ModelProvider provider, ChatRequest chatRequest) {
        return invokeStream(model, provider, chatRequest)
                .map(this::convertStreamChunk)
                .filter(chunk -> !chunk.isEmpty());
    }

    private StreamChunk convertStreamChunk(ChatResponse chatResponse) {
        StreamChunk.StreamChunkBuilder builder = StreamChunk.builder();
        //1.提取token信息
        if (ObjectUtil.isNotEmpty(chatResponse.getMetadata()) && chatResponse.getMetadata().getUsage() != null
        ) {
            Usage dashscopeUsage = chatResponse.getMetadata().getUsage();

            Integer promptTokens = dashscopeUsage.getPromptTokens();

            Integer totalTokens = dashscopeUsage.getTotalTokens();

            if (promptTokens != null && promptTokens > 0) {
                builder.promptTokens(promptTokens);
            }

            if (totalTokens != null && totalTokens > 0) {
                builder.completionTokens(totalTokens);
            }
        }
        //2.提取文本内容
        //提取深度思考内容,通义千问使用字段reasoningContent
        AssistantMessage assistantMessage = chatResponse.getResult().getOutput();

        Map<String, Object> metadata = assistantMessage.getMetadata();

        if (ObjectUtil.isNotEmpty(metadata)) {
            Object reasoningContent = metadata.get("reasoningContent");

            if (reasoningContent != null && !reasoningContent.toString().isEmpty()) {
                builder.reasoningContent(reasoningContent.toString());
            }
        }
        //提取普通文本内容
        String text = assistantMessage.getText();

        if (ObjectUtil.isNotEmpty(text)) {
            builder.text(text);
        }

        StreamChunk streamChunk = builder.build();
        //如果没有任何内容,标记为空
        if (!streamChunk.hasText() && !streamChunk.hasReasoningContent()
        && streamChunk.getPromptTokens() == null && streamChunk.getCompletionTokens() == null) {
            return StreamChunk.empty();
        }

        return streamChunk;
    }

    @Override
    public boolean supports(String providerName) {
        return providerName != null && SUPPORTED_MODEL_TYPES.contains(providerName.toLowerCase());
    }
}
