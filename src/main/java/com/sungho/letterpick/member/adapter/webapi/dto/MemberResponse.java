package com.sungho.letterpick.member.adapter.webapi.dto;

import com.sungho.letterpick.member.application.provided.MemberView;

import static java.util.Objects.requireNonNull;

public record MemberResponse(
        Long memberId,
        String email,
        String nickname,
        String status,
        String role,
        String newsletterInboxAddress
) {
    public static MemberResponse from(MemberView view) {
        requireNonNull(view);
        return new MemberResponse(
                view.memberId(),
                view.email(),
                view.nickname(),
                view.status().name(),
                view.role().name(),
                view.newsletterInboxAddress()
        );
    }
}
