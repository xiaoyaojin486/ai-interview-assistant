package com.example.interviewassistant.chat.application;

import org.springframework.stereotype.Component;

import com.example.interviewassistant.common.ai.prompt.PromptCatalog;

@Component
public class ChatPrompts {

    private static final String SHARED_SYSTEM_RULES = "common/system-rules";
    private static final String GENERAL_ANSWER = "chat/answer";

    private final PromptCatalog promptCatalog;

    public ChatPrompts(PromptCatalog promptCatalog) {
        this.promptCatalog = promptCatalog;
    }

    /**
     * 聊天用例只依赖类型化入口，不直接接触资源路径或提示词正文。
     */
    public String generalAnswer() {
        return promptCatalog.render(SHARED_SYSTEM_RULES)
                + System.lineSeparator()
                + System.lineSeparator()
                + promptCatalog.render(GENERAL_ANSWER);
    }
}
