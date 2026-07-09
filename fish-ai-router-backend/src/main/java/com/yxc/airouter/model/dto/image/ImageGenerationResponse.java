package com.yxc.airouter.model.dto.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图片生成响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerationResponse {
    
    /**
     * 创建时间戳
     */
    private Long created;
    
    /**
     * 图片数据列表
     */
    private List<ImageData> data;
    
    /**
     * 图片数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageData {
        /**
         * 图片URL
         */
        private String url;
        
        /**
         * Base64编码的图片数据
         */
        private String b64Json;
        
        /**
         * 修订后的提示词
         */
        private String revisedPrompt;
    }
}
