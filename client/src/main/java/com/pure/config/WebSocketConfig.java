package com.pure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * TODO
 *
 * @author gnl
 * @since 2023/5/3
 */
@Configuration
public class WebSocketConfig {

    // 检测所有带有 @ServerEndpoint 注解的 Bean 并注册到容器中
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
