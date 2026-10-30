package com.leon.agenthub.common;

import com.leon.agenthub.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
// import org.springframework.data.redis.core.StringRedisTemplate;   // Redis 依赖已临时注释
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口 — 供负载均衡、监控系统（Prometheus / 阿里云 SLB）探活使用。
 *
 * <p>GET /api/health → 返回各依赖组件的存活状态。
 */
@Tag(name = "健康检查", description = "应用存活与依赖组件状态探测")
@RestController
public class HealthController {

    private final DataSource dataSource;
    // private final StringRedisTemplate redisTemplate;                // Redis 依赖已临时注释

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Operation(summary = "完整健康检查", description = "检查 PostgreSQL、Redis 连接状态")
    @GetMapping("/health")
    public R<HealthResult> health() {
        HealthResult result = new HealthResult();
        result.setStatus("UP");
        result.setTimestamp(LocalDateTime.now().toString());

        Map<String, ComponentHealth> components = new LinkedHashMap<>();

        // 1. PostgreSQL 检查
        components.put("postgres", checkDatabase());

        // 2. Redis 检查 —— Redis 依赖已临时注释，恢复依赖后取消下方注释
        // components.put("redis", checkRedis());

        result.setComponents(components);

        // 任一组件异常则整体标记为 DOWN
        boolean allUp = components.values().stream()
                .allMatch(c -> "UP".equals(c.getStatus()));
        if (!allUp) {
            result.setStatus("DOWN");
        }

        return R.ok(result);
    }

    /**
     * 简易存活探针 — 仅返回 HTTP 200，用于不关心依赖的快速探活。
     */
    @Operation(summary = "快速存活探针", description = "仅返回 pong，用于负载均衡探活")
    @GetMapping("/health/ping")
    public R<String> ping() {
        return R.ok("pong");
    }

    private ComponentHealth checkDatabase() {
        ComponentHealth health = new ComponentHealth();
        try (Connection conn = dataSource.getConnection()) {
            if( conn.isValid(3)) {
                health.setStatus("UP");
                health.setDetail("connection valid");
            } else {
                health.setStatus("DOWN");
                health.setDetail("connection invalid");
            }
        } catch (Exception e) {
            health.setStatus("DOWN");
            health.setDetail(e.getMessage());
        }
        return health;
    }

    /**
     * Redis 连通性检查 —— Redis 依赖已临时注释，恢复依赖后取消下方注释
     */
    // private ComponentHealth checkRedis() {
    //     ComponentHealth health = new ComponentHealth();
    //     try {
    //         // 用真实读写操作验证连通性（Lettuce 的 ping 在健康检查场景不稳定）
    //         String testKey = "__healthcheck__";
    //         redisTemplate.opsForValue().set(testKey, "ok");
    //         String value = redisTemplate.opsForValue().get(testKey);
    //         redisTemplate.delete(testKey);
    //         if ("ok".equals(value)) {
    //             health.setStatus("UP");
    //             health.setDetail("read/write ok");
    //         } else {
    //             health.setStatus("DOWN");
    //             health.setDetail("unexpected value: " + value);
    //         }
    //     } catch (Exception e) {
    //         health.setStatus("DOWN");
    //         health.setDetail(e.getMessage());
    //     }
    //     return health;
    // }

    @Data
    public static class HealthResult {
        private String status;
        private String timestamp;
        private Map<String, ComponentHealth> components;
    }

    @Data
    public static class ComponentHealth {
        private String status;
        private String detail;
    }
}
