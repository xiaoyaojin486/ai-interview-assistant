package com.example.interviewassistant.chat.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_AI_TEST", matches = "true")
class DeepSeekLiveTest {

    @Autowired
    private ChatStreamingService chatStreamingService;

    @Value("${spring.ai.deepseek.api-key:}")
    private String apiKey;

    /**
     * 只有显式开启真实测试时才消耗 DeepSeek 额度。
     */
    @Test
    void receivesNonEmptyStreamInMultipleChunks() {
        List<String> chunks = chatStreamingService
                .stream("只回答：Spring AI 流式输出验证成功。")
                .collectList()
                .block(Duration.ofSeconds(90));

        assertThat(chunks).isNotNull();
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);

        String reply = String.join("", chunks);
        assertThat(reply).isNotBlank();
        assertThat(reply).doesNotContain(apiKey);
    }
}
