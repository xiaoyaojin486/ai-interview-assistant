package com.example.interviewassistant.chat.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import reactor.core.publisher.Flux;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FakeChatModelStreamingTest.FakeChatModelConfiguration.class)
class FakeChatModelStreamingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeChatModel fakeChatModel;

    /**
     * 使用假 ChatModel 验证完整 SSE 链路，不访问真实 DeepSeek。
     */
    @Test
    void streamsFakeModelResponseWithoutNetworkCall() throws Exception {
        MvcResult initialResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {"message":"使用假模型回答"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(initialResult))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        containsString("\"text\":\"fake \""),
                        containsString("\"text\":\"response\""),
                        containsString("event:done"))));

        org.assertj.core.api.Assertions.assertThat(fakeChatModel.streamCalls()).isEqualTo(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeChatModelConfiguration {

        @Bean
        @Primary
        FakeChatModel fakeChatModel() {
            return new FakeChatModel();
        }
    }

    static class FakeChatModel implements ChatModel {

        private final AtomicInteger streamCalls = new AtomicInteger();

        @Override
        public ChatResponse call(Prompt prompt) {
            return response("fake response");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            streamCalls.incrementAndGet();
            return Flux.just(response("fake "), response("response"));
        }

        int streamCalls() {
            return streamCalls.get();
        }

        private ChatResponse response(String text) {
            return new ChatResponse(List.of(
                    new Generation(new AssistantMessage(text))));
        }
    }
}
