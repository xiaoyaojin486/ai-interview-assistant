package com.example.interviewassistant.chat.application;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class ChatStreamErrorMapper {

    /**
     * 把底层异常映射为稳定、可操作且不泄露内部信息的 SSE 错误。
     */
    public ChatStreamError map(Throwable error) {
        boolean networkFailure = false;
        Throwable current = error;

        while (current != null) {
            if (current instanceof WebClientResponseException responseError) {
                int status = responseError.getStatusCode().value();
                if (status == 401 || status == 403) {
                    return new ChatStreamError(
                            "DEEPSEEK_AUTH",
                            "DeepSeek 鉴权失败，请检查 .env 中的 API Key。");
                }
                if (status == 429) {
                    return new ChatStreamError(
                            "DEEPSEEK_RATE_LIMIT",
                            "请求过于频繁，请稍后重试。");
                }
                if (status >= 500) {
                    return new ChatStreamError(
                            "DEEPSEEK_UNAVAILABLE",
                            "DeepSeek 服务暂时不可用，请稍后重试。");
                }
            }

            if (current instanceof TimeoutException
                    || current instanceof SocketTimeoutException) {
                return new ChatStreamError(
                        "DEEPSEEK_TIMEOUT",
                        "DeepSeek 响应超时，请稍后重试。");
            }

            if (current instanceof WebClientRequestException
                    || current instanceof IOException) {
                networkFailure = true;
            }
            current = current.getCause();
        }

        if (networkFailure) {
            return new ChatStreamError(
                    "DEEPSEEK_UNAVAILABLE",
                    "暂时无法连接 DeepSeek，请检查网络后重试。");
        }

        return new ChatStreamError(
                "DEEPSEEK_ERROR",
                "模型响应失败，请稍后重试。");
    }
}
