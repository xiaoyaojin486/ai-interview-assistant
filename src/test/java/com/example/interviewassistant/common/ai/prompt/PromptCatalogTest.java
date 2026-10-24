package com.example.interviewassistant.common.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class PromptCatalogTest {

    private final PromptCatalog promptCatalog = new PromptCatalog(new DefaultResourceLoader());

    /**
     * 验证固定提示词从 UTF-8 资源文件加载。
     */
    @Test
    void rendersPromptFromClasspathResource() {
        assertThat(promptCatalog.render("common/system-rules"))
                .contains("你是 AI 面试助手")
                .contains("不要输出 API Key");
    }

    /**
     * 验证带命名变量的提示词可以渲染。
     */
    @Test
    void rendersPromptVariables() {
        String rendered = promptCatalog.render(
                "test/greeting",
                Map.of("name", "面试候选人"));

        assertThat(rendered).isEqualTo("你好，面试候选人。");
    }

    /**
     * 验证同一个提示词名称复用已加载的模板实例。
     */
    @Test
    void cachesPromptTemplate() {
        assertThat(promptCatalog.template("chat/answer"))
                .isSameAs(promptCatalog.template("chat/answer"));
    }

    /**
     * 拒绝不符合约定和可能造成路径穿越的名称。
     */
    @Test
    void rejectsInvalidPromptName() {
        assertThatThrownBy(() -> promptCatalog.render("../secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prompt name");

        assertThatThrownBy(() -> promptCatalog.render("Chat/Answer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prompt name");

        assertThatThrownBy(() -> promptCatalog.render("chat//answer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prompt name");

        assertThatThrownBy(() -> promptCatalog.render("/chat/answer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prompt name");
    }

    /**
     * 缺失资源时快速失败，避免使用不完整的系统规则。
     */
    @Test
    void rejectsMissingPromptResource() {
        assertThatThrownBy(() -> promptCatalog.render("chat/missing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Prompt resource not found");
    }
}
