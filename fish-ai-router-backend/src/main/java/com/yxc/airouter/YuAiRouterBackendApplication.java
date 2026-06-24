package com.yxc.airouter;

import com.alibaba.cloud.ai.autoconfigure.dashscope.*;
import org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = {
        // 排除 OpenAI 自动配置
        OpenAiChatAutoConfiguration.class,
        OpenAiEmbeddingAutoConfiguration.class,
        OpenAiImageAutoConfiguration.class,
        OpenAiAudioSpeechAutoConfiguration.class,
        OpenAiAudioTranscriptionAutoConfiguration.class,
        OpenAiModerationAutoConfiguration.class,
        // 排除 DeepSeek 自动配置
        DeepSeekChatAutoConfiguration.class,
        // 排除 DashScope 自动配置
        DashScopeChatAutoConfiguration.class,
        DashScopeAgentAutoConfiguration.class,
        DashScopeEmbeddingAutoConfiguration.class,
        DashScopeImageAutoConfiguration.class,
        DashScopeAudioSpeechAutoConfiguration.class,
        DashScopeAudioTranscriptionAutoConfiguration.class,
        DashScopeRerankAutoConfiguration.class,
        DashScopeVideoAutoConfiguration.class
})
@EnableAsync
public class YuAiRouterBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuAiRouterBackendApplication.class, args);
    }

}
