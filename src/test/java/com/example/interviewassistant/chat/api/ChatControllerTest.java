package com.example.interviewassistant.chat.api;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.interviewassistant.chat.application.ChatStreamingService;

import reactor.core.publisher.Flux;

@WebMvcTest(ChatController.class)
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
}
