package com.sungho.letterpick.member.application.provided;

import com.sungho.letterpick.member.domain.Member;
import com.sungho.letterpick.member.domain.MemberRole;
import com.sungho.letterpick.member.domain.MemberStatus;

import static java.util.Objects.requireNonNull;

public record MemberView(
        Long memberId,
        String email,
        String nickname,
        MemberStatus status,
        MemberRole role,
        String newsletterInboxAddress
) {
    public static MemberView from(Member member) {
        requireNonNull(member);
        return new MemberView(
                member.getId(),
                member.getEmail().address(),
                member.getNickname().name(),
                member.getStatus(),
                member.getRole(),
                member.getNewsletterInboxAddress().address()
        );
    }
}
