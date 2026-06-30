package com.fishstar.ai.router.sdk;

import com.fishstar.ai.sdk.YuAIClient;
import com.fishstar.ai.sdk.model.ChatMessage;
import com.fishstar.ai.sdk.model.ChatRequest;
import com.fishstar.ai.sdk.model.ChatResponse;

import java.util.Arrays;

public class AdvancedExample {
    public static void main(String[] args) {
        // 完整配置
        YuAIClient client = YuAIClient.builder()
                .apiKey("sk-6220446269d547238dcf642bde5b5396")
                .baseUrl("http://localhost:8123/api")
                .connectTimeout(15000)     // 连接超时 15秒
                .readTimeout(60000)        // 读取超时 60秒
                .maxRetries(5)             // 最多重试 5次
                .retryDelay(2000)          // 重试延迟 2秒
                .build();
        
        try {
            System.out.println("=== 多轮对话示例 ===\n");

            ChatRequest request = ChatRequest.builder()
                    .messages(Arrays.asList(
                            ChatMessage.system("你是一个编程助手"),
                            ChatMessage.user("什么是 Java？"),
                            ChatMessage.assistant("Java 是一种面向对象的编程语言..."),
                            ChatMessage.user("它的主要特点是什么？")
                    ))
                    .model("qwen-turbo")
                    .temperature(0.7)
                    .build();

            ChatResponse response = client.chat(request);
            System.out.println("回答: " + response.getContent());

        } finally {
            client.close();
        }
    }
}
