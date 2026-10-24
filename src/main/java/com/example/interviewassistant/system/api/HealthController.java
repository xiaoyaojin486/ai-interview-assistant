package com.example.interviewassistant.system.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 返回最小健康信息，用于确认 Spring Boot 已启动并能处理 HTTP 请求。
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        // 使用 LinkedHashMap 保持字段输出顺序，方便本地调试和查看响应。
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("application", "AI Interview Assistant");
        response.put("timestamp", Instant.now().toString());
        return response;
    }
}
