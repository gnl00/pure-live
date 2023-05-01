package com.pure;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class MainTest {

    @Value("${spring.application.name}")
    private String applicationName;

    @Test
    public void testMain() {
        log.info("spring.application.name {}", applicationName);
    }

}
