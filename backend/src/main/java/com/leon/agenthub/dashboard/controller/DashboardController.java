package com.leon.agenthub.dashboard.controller;

import com.leon.agenthub.common.response.R;
import com.leon.agenthub.dashboard.dto.DashboardStatsVO;
import com.leon.agenthub.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台控制器 — 提供工作台统计数据接口。
 *
 * <p>分层约定：本层只做转发与响应包装，统计口径与权限过滤全部在 Service 层完成。
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 查询工作台统计数据。
     *
     * @return 工作台统计数据（文档、问答、智能体、活跃度、趋势、最近列表）
     */
    @GetMapping("/stats")
    public R<DashboardStatsVO> stats() {
        return R.ok(dashboardService.stats());
    }
}
