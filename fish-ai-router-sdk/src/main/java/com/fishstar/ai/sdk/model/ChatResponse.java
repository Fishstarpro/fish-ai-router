package com.fishstar.ai.sdk.model;

import lombok.Data;

import java.util.List;

@Data
public class ChatResponse {
    
    private String id;                     // 响应 ID（如 "chatcmpl-xxx"）
    private String object;                 // 对象类型（固定为 "chat.completion"）
    private Long created;                  // 时间戳（秒）
    private String model;                  // 使用的模型
    private List<Choice> choices;          // 选项列表
    private Usage usage;                   // Token 使用情况
    
    @Data
    public static class Choice {
        private Integer index;              // 选项索引，默认 0
        private ChatMessage message;        // AI 消息
        private String finishReason;        // 结束原因（"stop" / "length" / "content_filter"）
    }
    
    @Data
    public static class Usage {
        private Integer promptTokens;       // 输入 Token
        private Integer completionTokens;   // 输出 Token
        private Integer totalTokens;        // 总 Token
    }
    
    // 便捷方法：直接获取回复内容
    public String getContent() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        ChatMessage message = choices.get(0).getMessage();
        return message != null ? message.getContent() : "";
    }
}
