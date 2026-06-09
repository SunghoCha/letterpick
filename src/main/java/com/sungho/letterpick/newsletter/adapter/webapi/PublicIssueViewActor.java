package com.sungho.letterpick.newsletter.adapter.webapi;

import java.util.Objects;

public record PublicIssueViewActor(String actorKey) {

    public PublicIssueViewActor {
        Objects.requireNonNull(actorKey, "actorKey must not be null");
        if (actorKey.isBlank()) {
            throw new IllegalArgumentException("actorKey must not be blank");
        }
    }
}
