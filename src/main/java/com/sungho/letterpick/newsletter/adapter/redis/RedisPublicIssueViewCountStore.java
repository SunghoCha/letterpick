package com.sungho.letterpick.newsletter.adapter.redis;

import com.sungho.letterpick.newsletter.application.PublicIssueViewCountProperties;
import com.sungho.letterpick.newsletter.application.required.PublicIssueViewCountStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisPublicIssueViewCountStore implements PublicIssueViewCountStore {

    private static final String VIEW_COUNT_KEY_PART = "view-count";
    private static final String VIEW_DEDUPE_KEY_PART = "view-dedupe";
    private static final String VIEWED_MARK = "1";
    private static final RedisScript<Long> INCREMENT_IF_FIRST_VIEW_SCRIPT = RedisScript.of("""
            if redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) then
                return redis.call('INCR', KEYS[2])
            end

            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final PublicIssueViewCountProperties properties;

    @Override
    public long incrementIfFirstView(Long issueId, String actorKey) {
        Objects.requireNonNull(issueId, "issueId must not be null");
        Objects.requireNonNull(actorKey, "actorKey must not be null");
        if (actorKey.isBlank()) {
            throw new IllegalArgumentException("actorKey must not be blank");
        }

        Long viewCount = redisTemplate.execute(
                INCREMENT_IF_FIRST_VIEW_SCRIPT,
                List.of(dedupeKey(issueId, actorKey), viewCountKey(issueId)),
                VIEWED_MARK,
                String.valueOf(properties.dedupeTtl().toMillis())
        );
        if (viewCount == null) {
            throw new IllegalStateException("Redis did not return view count script result");
        }
        return viewCount;
    }

    private String viewCountKey(Long issueId) {
        return String.join(":", issueKeyPrefix(issueId), VIEW_COUNT_KEY_PART);
    }

    private String dedupeKey(Long issueId, String actorKey) {
        return String.join(":", issueKeyPrefix(issueId), VIEW_DEDUPE_KEY_PART, actorHash(actorKey));
    }

    private String issueKeyPrefix(Long issueId) {
        return String.join(":", properties.redisKeyPrefix(), "{" + issueId + "}");
    }

    private String actorHash(String actorKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(actorKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is not available", e);
        }
    }
}
