package com.sungho.letterpick;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LetterPickApplication {

    public static void main(String[] args) {
        SpringApplication.run(LetterPickApplication.class, args);
    }

}
