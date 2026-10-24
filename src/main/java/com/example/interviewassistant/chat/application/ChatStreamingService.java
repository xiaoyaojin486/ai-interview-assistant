package com.example.interviewassistant.chat.application;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class ChatStreamingService {

    private final ChatClient chatClient;
    private final ChatPrompts chatPrompts;

    /**
     * 使用自动配置的 ChatClient 调用固定的 DeepSeek 模型。
     */
    public ChatStreamingService(
            ChatClient.Builder chatClientBuilder,
            ChatPrompts chatPrompts) {
        this.chatClient = chatClientBuilder.build();
        this.chatPrompts = chatPrompts;
    }

    /**
     * 返回 DeepSeek 生成的连续文本片段，交由 Web 层转换为 SSE 事件。
     */
    public Flux<String> stream(String message) {
        return chatClient.prompt()
                .system(chatPrompts.generalAnswer())
                .user(message)
                .stream()
                .content();
    }
}
