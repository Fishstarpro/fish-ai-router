package com.fishstar.ai.router.sdk;

import com.fishstar.ai.sdk.YuAIClient;
import com.fishstar.ai.sdk.model.ChatResponse;

/**
 * 最简单的使用示例
 */
public class SimpleExample {

    public static void main(String[] args) {
        // 创建客户端
        YuAIClient client = YuAIClient.builder()
                .apiKey("sk-6220446269d547238dcf642bde5b5396")  // 替换为你的 API Key
                .baseUrl("http://localhost:8123/api")
                .build();

        try {
            // 同步调用
            ChatResponse response = client.chat("你好，请介绍一下自己");
            System.out.println("响应: " + response.getContent());

            // Token 使用统计
            System.out.println("\nToken 统计:");
            System.out.println("输入: " + response.getUsage().getPromptTokens());
            System.out.println("输出: " + response.getUsage().getCompletionTokens());
            System.out.println("总计: " + response.getUsage().getTotalTokens());

        } finally {
            // 关闭客户端
            client.close();
        }
    }
}