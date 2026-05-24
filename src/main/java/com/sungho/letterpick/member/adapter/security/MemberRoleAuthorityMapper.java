package com.sungho.letterpick.member.adapter.security;

import static com.sungho.letterpick.common.auth.SecurityAuthorities.ROLE_ADMIN;
import static com.sungho.letterpick.common.auth.SecurityAuthorities.ROLE_USER;
import static java.util.Objects.requireNonNull;

import com.sungho.letterpick.member.domain.MemberRole;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

final class MemberRoleAuthorityMapper {

    private MemberRoleAuthorityMapper() {
    }

    static List<GrantedAuthority> from(MemberRole role) {
        return switch (requireNonNull(role)) {
            case USER -> List.of(new SimpleGrantedAuthority(ROLE_USER));
            case ADMIN -> List.of(
                    new SimpleGrantedAuthority(ROLE_USER),
                    new SimpleGrantedAuthority(ROLE_ADMIN)
            );
        };
    }
}
