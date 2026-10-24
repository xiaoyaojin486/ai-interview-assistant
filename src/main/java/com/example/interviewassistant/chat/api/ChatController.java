package com.example.interviewassistant.chat.api;

import java.util.Map;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.interviewassistant.chat.application.ChatStreamingService;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatStreamingService chatStreamingService;

    public ChatController(ChatStreamingService chatStreamingService) {
        this.chatStreamingService = chatStreamingService;
    }

    /**
     * 把 DeepSeek 的文本流转换为浏览器可消费的 SSE 事件。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> stream(
            @Valid @RequestBody ChatRequest request) {
        return chatStreamingService.stream(request.message().trim())
                .map(text -> event("delta", Map.of("text", text)))
                .concatWithValues(event("done", Map.of()))
                .onErrorResume(error -> {
                    // 只记录异常类型，避免把完整 Prompt、密钥或底层响应写入日志。
                    log.warn("DeepSeek stream failed: {}", error.getClass().getSimpleName());
                    return Flux.just(event("error", Map.of(
                            "code", "DEEPSEEK_ERROR",
                            "message", "模型响应失败，请稍后重试")));
                });
    }

    private ServerSentEvent<Map<String, Object>> event(
            String name,
            Map<String, Object> data) {
        return ServerSentEvent.<Map<String, Object>>builder()
                .event(name)
                .data(data)
                .build();
    }

}
