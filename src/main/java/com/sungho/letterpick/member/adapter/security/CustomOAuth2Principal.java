package com.sungho.letterpick.member.adapter.security;

import static com.sungho.letterpick.common.auth.SecurityAuthorities.ROLE_PENDING_SIGNUP;

import com.sungho.letterpick.common.auth.SocialPrincipal;
import com.sungho.letterpick.common.auth.SocialUserInfo;
import com.sungho.letterpick.member.domain.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CustomOAuth2Principal implements OAuth2User, SocialPrincipal, Serializable {

    private final Member member;
    private final SocialUserInfo socialUserInfo;
    private final OAuth2User delegate;
    private final Collection<? extends GrantedAuthority> authorities;

    private CustomOAuth2Principal(Member member,
                                  SocialUserInfo socialUserInfo,
                                  OAuth2User delegate,
                                  Collection<? extends GrantedAuthority> authorities) {
        this.member = member;
        this.socialUserInfo = socialUserInfo;
        this.delegate = delegate;
        this.authorities = authorities;
    }

    public static CustomOAuth2Principal existing(Member member, OAuth2User delegate) {
        return new CustomOAuth2Principal(
                member,
                null,
                delegate,
                MemberRoleAuthorityMapper.from(member.getRole())
        );
    }

    public static CustomOAuth2Principal pending(SocialUserInfo socialUserInfo, OAuth2User delegate) {
        return new CustomOAuth2Principal(
                null,
                socialUserInfo,
                delegate,
                List.of(new SimpleGrantedAuthority(ROLE_PENDING_SIGNUP))
        );
    }

    public boolean isPending() {
        return member == null;
    }

    @Override
    public SocialPrincipal withMember(Member newMember) {
        return CustomOAuth2Principal.existing(newMember, this.delegate);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}
