package com.sungho.letterpick.member.domain;

import com.sungho.letterpick.common.domain.Email;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.sungho.letterpick.member.domain.exception.MemberStatusException;

import static java.util.Objects.requireNonNull;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_member_nickname", columnNames = "nickname"),
        @UniqueConstraint(name = "uk_member_newsletter_inbox_address", columnNames = "newsletter_inbox_address"),
        @UniqueConstraint(name = "uk_member_social_identity",
                columnNames = {"social_provider", "social_provider_id"})
})
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "address",
            column = @Column(name = "email", nullable = false, length = 100))
    private Email email;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "socialProvider",
                    column = @Column(name = "social_provider", nullable = false, length = 20)),
            @AttributeOverride(name = "socialProviderId",
                    column = @Column(name = "social_provider_id", nullable = false, length = 100))
    })
    private SocialIdentity socialIdentity;

    @Embedded
    @AttributeOverride(name = "name",
            column = @Column(name = "nickname", nullable = false, length = 20))
    private Nickname nickname;

    @Embedded
    @AttributeOverride(name = "address",
            column = @Column(name = "newsletter_inbox_address", nullable = false, length = 254))
    private NewsletterInboxAddress newsletterInboxAddress;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    private Member(Email email, Nickname nickname, SocialIdentity socialIdentity,
                   NewsletterInboxAddress newsletterInboxAddress) {
        this.email = requireNonNull(email);
        this.nickname = requireNonNull(nickname);
        this.socialIdentity = requireNonNull(socialIdentity);
        this.newsletterInboxAddress = requireNonNull(newsletterInboxAddress);
        this.status = MemberStatus.ACTIVE;
    }

    public static Member register(Email email, Nickname nickname, SocialIdentity socialIdentity,
                                  NewsletterInboxAddress newsletterInboxAddress) {
        return new Member(email, nickname, socialIdentity, newsletterInboxAddress);
    }

    public void changeNickname(Nickname nickname) {
        ensureCanChangeNickname();
        this.nickname = requireNonNull(nickname);
    }

    public void suspend() {
        if (this.status != MemberStatus.ACTIVE) {
            throw new MemberStatusException();
        }
        this.status = MemberStatus.SUSPENDED;
    }

    public void withdraw() {
        if (this.status != MemberStatus.ACTIVE) {
            throw new MemberStatusException();
        }
        this.status = MemberStatus.DEACTIVATED;
    }

    public void withdrawByAdmin() {
        if (this.status != MemberStatus.SUSPENDED) {
            throw new MemberStatusException();
        }
        this.status = MemberStatus.DEACTIVATED;
    }

    public void ensureCanChangeNickname() {
        if (this.status != MemberStatus.ACTIVE) {
            throw new MemberStatusException();
        }
    }

    public boolean canLogin() {
        return this.status == MemberStatus.ACTIVE;
    }
}
