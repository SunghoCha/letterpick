package com.sungho.letterpick.trending;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TrendingServiceTestConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class TrendingServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
