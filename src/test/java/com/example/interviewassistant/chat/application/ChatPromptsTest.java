package com.example.interviewassistant.chat.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.example.interviewassistant.common.ai.prompt.PromptCatalog;

class ChatPromptsTest {

    /**
     * 验证聊天提示词按共享系统规则、聊天任务的顺序组合。
     */
    @Test
    void composesSharedRulesAndChatTask() {
        PromptCatalog promptCatalog = new PromptCatalog(new DefaultResourceLoader());
        ChatPrompts chatPrompts = new ChatPrompts(promptCatalog);

        String prompt = chatPrompts.generalAnswer();

        int sharedRulesIndex = prompt.indexOf("你是 AI 面试助手");
        int chatTaskIndex = prompt.indexOf("请直接回答用户问题");

        assertThat(sharedRulesIndex).isGreaterThanOrEqualTo(0);
        assertThat(chatTaskIndex).isGreaterThan(sharedRulesIndex);
        assertThat(prompt).contains("不要输出 API Key");
    }
}
