package com.sungho.letterpick.support.time;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class MutableClockTestConfiguration {

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return MutableClock.fixedAt("2050-01-01T00:00:00Z");
    }
}
