package com.yxc.airouter.controller;

import com.yxc.airouter.annotation.AuthCheck;
import com.yxc.airouter.common.BaseResponse;
import com.yxc.airouter.common.ResultUtils;
import com.yxc.airouter.constant.UserConstant;
import com.yxc.airouter.model.entity.RequestLog;
import com.yxc.airouter.model.entity.User;
import com.yxc.airouter.service.RequestLogService;
import com.yxc.airouter.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.List;

/**
 * 统计接口
 *
 * @author <a href="https://codefather.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Resource
    private RequestLogService requestLogService;

    @Resource
    private UserService userService;

    /**
     * 获取我的 Token 消耗统计
     */
    @GetMapping("/my/tokens")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "获取我的 Token 消耗统计")
    public BaseResponse<TokenStatsVO> getMyTokenStats(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long totalTokens = requestLogService.countUserTokens(loginUser.getId());

        TokenStatsVO vo = new TokenStatsVO();
        vo.setTotalTokens(totalTokens);

        return ResultUtils.success(vo);
    }

    /**
     * 获取我的请求日志
     */
    @GetMapping("/my/logs")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    @Operation(summary = "获取我的请求日志")
    public BaseResponse<List<RequestLog>> getMyLogs(@RequestParam(defaultValue = "100") Integer limit,
                                                    HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<RequestLog> logs = requestLogService.listUserLogs(loginUser.getId(), limit);

        return ResultUtils.success(logs);
    }

    /**
     * Token 统计视图对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenStatsVO implements Serializable {
        /**
         * 总Token数
         */
        private Long totalTokens;
    }
}