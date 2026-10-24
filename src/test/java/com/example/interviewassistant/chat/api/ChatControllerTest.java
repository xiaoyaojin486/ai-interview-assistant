package com.example.interviewassistant.chat.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.interviewassistant.chat.application.ChatStreamErrorMapper;
import com.example.interviewassistant.chat.application.ChatStreamingService;

import reactor.core.publisher.Flux;

@WebMvcTest(ChatController.class)
@Import(ChatStreamErrorMapper.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatStreamingService chatStreamingService;

    /**
     * 验证服务返回的多个文本片段被包装成 delta，并在末尾发送 done。
     */
    @Test
    void streamsDeltasAndDoneEvent() throws Exception {
        when(chatStreamingService.stream("解释 Spring AI"))
                .thenReturn(Flux.just("Spring ", "AI"));

        MvcResult initialResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {"message":"解释 Spring AI"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(initialResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(allOf(
                        containsString("event:delta"),
                        containsString("\"text\":\"Spring \""),
                        containsString("\"text\":\"AI\""),
                        containsString("event:done"))));
    }

    /**
     * 验证空消息在进入模型服务前就返回 400。
     */
    @Test
    void rejectsBlankMessageWithoutCallingModel() throws Exception {
        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"   "}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatStreamingService);
    }

    /**
     * 验证超过 2000 字的请求在进入模型前返回可读错误。
     */
    @Test
    void rejectsMessageLongerThanLimitWithoutCallingModel() throws Exception {
        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"%s"}
                                """.formatted("a".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("消息不能超过 2000 个字符"));

        verifyNoInteractions(chatStreamingService);
    }

    /**
     * 验证 2000 字边界仍然允许发送。
     */
    @Test
    void acceptsMessageAtCharacterLimit() throws Exception {
        String message = "a".repeat(2000);
        when(chatStreamingService.stream(message)).thenReturn(Flux.just("ok"));

        MvcResult initialResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {"message":"%s"}
                                """.formatted(message)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(initialResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:done")));
    }

    /**
     * 验证模型异常被转换为不含底层敏感信息的 error 事件。
     */
    @Test
    void mapsModelFailureToGenericErrorEvent() throws Exception {
        when(chatStreamingService.stream("触发错误"))
                .thenReturn(Flux.error(new IllegalStateException("sensitive-upstream-detail")));

        MvcResult initialResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {"message":"触发错误"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(initialResult))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("event:error"),
                        containsString("\"code\":\"DEEPSEEK_ERROR\""),
                        not(containsString("sensitive-upstream-detail")),
                        not(containsString("event:done")))));
    }

    /**
     * 验证鉴权失败转换为可操作的 API Key 提示。
     */
    @Test
    void mapsAuthenticationFailureToActionableErrorEvent() throws Exception {
        when(chatStreamingService.stream("鉴权失败"))
                .thenReturn(Flux.error(org.springframework.web.reactive.function.client
                        .WebClientResponseException.create(
                                401,
                                "Unauthorized",
                                org.springframework.http.HttpHeaders.EMPTY,
                                new byte[0],
                                java.nio.charset.StandardCharsets.UTF_8)));

        MvcResult initialResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("""
                                {"message":"鉴权失败"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult completedResult = mockMvc.perform(asyncDispatch(initialResult))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(completedResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("\"code\":\"DEEPSEEK_AUTH\"")
                .contains("检查 .env 中的 API Key");
    }
}
