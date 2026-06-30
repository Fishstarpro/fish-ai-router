package com.fishstar.ai.sdk.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StreamResponse {
    
    private String id;                      // 响应 ID（如 "chatcmpl-xxx"）
    private String object;                  // 对象类型（固定为 "chat.completion.chunk"）
    private Long created;                   // 时间戳（秒）
    private String model;                   // 使用的模型
    private List<StreamChoice> choices;     // 选项列表
    
    @Data
    @Builder
    public static class StreamChoice {
        private Integer index;                   // 选项索引，默认 0
        private Delta delta;                     // 增量内容
        private String finishReason;             // 结束原因（null 表示未结束，"stop" 表示完成）
    }
    
    @Data
    @Builder
    public static class Delta {
        private String role;                     // 角色（仅首个块包含，值为 "assistant"）
        private String content;                  // 答案内容（增量）
        private String reasoningContent;         // 思考过程（增量，deepseek-reasoner 专属）
    }
}
