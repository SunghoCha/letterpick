package com.sungho.letterpick.member.adapter.security;

import static com.sungho.letterpick.common.auth.SecurityAuthorities.ROLE_PENDING_SIGNUP;

import com.sungho.letterpick.common.auth.SocialPrincipal;
import com.sungho.letterpick.common.auth.SocialUserInfo;
import com.sungho.letterpick.member.domain.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CustomOidcPrincipal implements OidcUser, SocialPrincipal, Serializable {

    private final Member member;
    private final SocialUserInfo socialUserInfo;
    private final OidcUser delegate;
    private final Collection<? extends GrantedAuthority> authorities;

    private CustomOidcPrincipal(Member member,
                                SocialUserInfo socialUserInfo,
                                OidcUser delegate,
                                Collection<? extends GrantedAuthority> authorities) {
        this.member = member;
        this.socialUserInfo = socialUserInfo;
        this.delegate = delegate;
        this.authorities = authorities;
    }

    public static CustomOidcPrincipal existing(Member member, OidcUser delegate) {
        return new CustomOidcPrincipal(
                member,
                null,
                delegate,
                MemberRoleAuthorityMapper.from(member.getRole())
        );
    }

    public static CustomOidcPrincipal pending(SocialUserInfo socialUserInfo, OidcUser delegate) {
        return new CustomOidcPrincipal(
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
        return CustomOidcPrincipal.existing(newMember, this.delegate);
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
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
