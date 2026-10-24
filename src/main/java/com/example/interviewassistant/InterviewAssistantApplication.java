package com.example.interviewassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InterviewAssistantApplication {

    /**
     * 应用启动入口，负责启动 Spring 容器和当前配置的 Web 服务。
     */
    public static void main(String[] args) {
        SpringApplication.run(InterviewAssistantApplication.class, args);
    }
}
