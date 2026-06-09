package com.sungho.letterpick.newsletter.adapter.webapi;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidAnonymousIdGenerator implements AnonymousIdGenerator {

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
