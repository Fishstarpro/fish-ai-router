package com.fishstar.ai.router.sdk;

import com.fishstar.ai.sdk.YuAIClient;
import com.fishstar.ai.sdk.callback.StreamCallback;
import com.fishstar.ai.sdk.model.ChatChunk;
import com.fishstar.ai.sdk.model.ChatMessage;
import com.fishstar.ai.sdk.model.ChatRequest;

import java.util.Arrays;

public class ReasoningExample {

    public static void main(String[] args) throws InterruptedException {
        YuAIClient client = YuAIClient.builder()
                .apiKey("sk-6220446269d547238dcf642bde5b5396")
                .baseUrl("http://localhost:8123/api")
                .build();

        try {
            System.out.println("=== 深度思考模式示例 ===\n");

            // 构建启用深度思考的请求
            ChatRequest request = ChatRequest.builder()
                    .model("qwen-plus")  // 支持深度思考的模型
                    .messages(Arrays.asList(
                            ChatMessage.user("请详细解释量子纠缠现象，并给出实际应用")
                    ))
                    .enableReasoning(true)   // 启用深度思考
                    .temperature(0.7)
                    .build();

            client.chatStream(request, new StreamCallback() {
                @Override
                public void onMessage(ChatChunk chunk) {
                    // 分别处理思考内容和普通内容
                    if (chunk.getReasoningContent() != null) {
                        System.out.println("\n💭 [思考] " + chunk.getReasoningContent());
                    }

                    if (chunk.getContent() != null) {
                        System.out.print(chunk.getContent());
                    }
                }

                @Override
                public void onComplete() {
                    System.out.println("\n\n✅ 完成");
                }

                @Override
                public void onError(Throwable error) {
                    System.err.println("\n❌ 错误: " + error.getMessage());
                    error.printStackTrace();
                }
            });

            // 等待流式响应完成
            Thread.sleep(30000);

        } finally {
            client.close();
        }
    }
}
