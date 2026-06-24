package com.yxc.airouter.adapter;

import com.yxc.airouter.exception.BusinessException;
import com.yxc.airouter.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ModelAdapterFactory {

    @Resource
    List<ModelAdapter> modelAdapters;

    public ModelAdapter getAdapter(String providerName) {
        //1.校验
        if (providerName == null || providerName.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提供者姓名不能为空");
        }
        //2.遍历拿到adapter
        for (ModelAdapter modelAdapter : modelAdapters) {
            if (modelAdapter.supports(providerName)) {
                log.debug("为提供者 {} 选择是适配器: {}", providerName, modelAdapter.getClass().getSimpleName());

                return modelAdapter;
            }
        }

        log.warn("未找到提供者 {} 的适配器, 将使用OpenAI兼容适配器", providerName);

        return modelAdapters.stream()
                .filter(adapter -> adapter instanceof OpenAIAdapter)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SYSTEM_ERROR, "未找到合适的适配器"));
    }
}
