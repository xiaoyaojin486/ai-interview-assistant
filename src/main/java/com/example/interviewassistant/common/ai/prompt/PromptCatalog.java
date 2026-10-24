package com.example.interviewassistant.common.ai.prompt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class PromptCatalog {

    private static final String PROMPT_ROOT = "classpath:prompts/";
    private static final String PROMPT_SUFFIX = ".st";
    private static final Pattern PROMPT_NAME = Pattern.compile("[a-z0-9-]+(?:/[a-z0-9-]+)*");

    private final ResourceLoader resourceLoader;
    private final ConcurrentMap<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    public PromptCatalog(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 渲染不带变量的提示词。
     */
    public String render(String name) {
        return template(name).render().strip();
    }

    /**
     * 使用命名变量渲染提示词，供后续计划、评分和追问场景复用。
     */
    public String render(String name, Map<String, Object> variables) {
        return template(name).render(variables).strip();
    }

    PromptTemplate template(String name) {
        if (name == null || !PROMPT_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid prompt name: " + name);
        }

        return templates.computeIfAbsent(name, this::load);
    }

    private PromptTemplate load(String name) {
        Resource resource = resourceLoader.getResource(PROMPT_ROOT + name + PROMPT_SUFFIX);
        if (!resource.exists()) {
            throw new IllegalStateException("Prompt resource not found: " + resource.getDescription());
        }
        return new PromptTemplate(resource);
    }
}
