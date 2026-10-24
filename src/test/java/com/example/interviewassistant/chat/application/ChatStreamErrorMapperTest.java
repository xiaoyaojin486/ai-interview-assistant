package com.example.interviewassistant.chat.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class ChatStreamErrorMapperTest {

    private final ChatStreamErrorMapper mapper = new ChatStreamErrorMapper();

    @Test
    void mapsAuthenticationFailure() {
        ChatStreamError error = mapper.map(responseError(401));

        assertThat(error.code()).isEqualTo("DEEPSEEK_AUTH");
        assertThat(error.message()).contains("API Key");
    }

    @Test
    void mapsRateLimitFailure() {
        ChatStreamError error = mapper.map(responseError(429));

        assertThat(error.code()).isEqualTo("DEEPSEEK_RATE_LIMIT");
        assertThat(error.message()).contains("稍后重试");
    }

    @Test
    void mapsServerFailure() {
        ChatStreamError error = mapper.map(responseError(503));

        assertThat(error.code()).isEqualTo("DEEPSEEK_UNAVAILABLE");
        assertThat(error.message()).contains("暂时不可用");
    }

    @Test
    void mapsTimeoutFailure() {
        ChatStreamError error = mapper.map(new TimeoutException("upstream timeout"));

        assertThat(error.code()).isEqualTo("DEEPSEEK_TIMEOUT");
    }

    @Test
    void mapsNetworkFailureWithoutExposingDetails() {
        ChatStreamError connectFailure = mapper.map(
                new RuntimeException(new ConnectException("connection refused")));
        ChatStreamError dnsFailure = mapper.map(new UnknownHostException("api.deepseek.com"));

        assertThat(connectFailure.code()).isEqualTo("DEEPSEEK_UNAVAILABLE");
        assertThat(dnsFailure.code()).isEqualTo("DEEPSEEK_UNAVAILABLE");
        assertThat(connectFailure.message()).doesNotContain("connection refused");
        assertThat(dnsFailure.message()).doesNotContain("api.deepseek.com");
    }

    @Test
    void mapsUnknownFailureToGenericError() {
        ChatStreamError error = mapper.map(new IllegalStateException("internal detail"));

        assertThat(error.code()).isEqualTo("DEEPSEEK_ERROR");
        assertThat(error.message()).doesNotContain("internal detail");
    }

    private WebClientResponseException responseError(int status) {
        return WebClientResponseException.create(
                status,
                "Upstream error",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8);
    }
}
