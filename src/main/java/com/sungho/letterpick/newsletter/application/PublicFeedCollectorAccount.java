package com.sungho.letterpick.newsletter.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublicFeedCollectorAccount {

    private final String collectorInboxAddress;
    private final RecipientAddressResolver recipientAddressResolver;
    private final Object collectorMemberIdLock = new Object();
    private volatile Long collectorMemberId;

    public PublicFeedCollectorAccount(@Value("${newsletter.public-feed.collector-inbox-address}")
                                      String collectorInboxAddress,
                                      RecipientAddressResolver recipientAddressResolver) {
        this.collectorInboxAddress = collectorInboxAddress;
        this.recipientAddressResolver = recipientAddressResolver;
    }

    public Long collectorMemberId() {
        Long cached = collectorMemberId;
        if (cached != null) {
            return cached;
        }

        synchronized (collectorMemberIdLock) {
            Long resolved = collectorMemberId;
            if (resolved == null) {
                resolved = resolveCollectorMemberId();
                collectorMemberId = resolved;
            }
            return resolved;
        }
    }

    private Long resolveCollectorMemberId() {
        RecipientAddressResolution resolution = recipientAddressResolver.resolve(collectorInboxAddress);
        if (resolution.type() != RecipientAddressResolution.Type.FOUND) {
            throw new IllegalStateException("공개 피드 컬렉터 회원을 찾을 수 없습니다.");
        }
        return resolution.memberId();
    }

    public boolean isCollectorMemberId(Long memberId) {
        return collectorMemberId().equals(memberId);
    }

    public boolean isCollectorInboxAddress(String recipientAddress) {
        return collectorInboxAddress.equals(recipientAddress);
    }
}
