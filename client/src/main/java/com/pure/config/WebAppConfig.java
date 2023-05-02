package com.pure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * TODO
 *
 * @author gnl
 * @since 2023/5/2
 */
@Configuration
@Slf4j
public class WebAppConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedHeaders("*")
                .allowedMethods("*")
                .allowCredentials(true);
    }

}
