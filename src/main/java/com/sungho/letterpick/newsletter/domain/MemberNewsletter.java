package com.sungho.letterpick.newsletter.domain;

import com.sungho.letterpick.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import static com.sungho.letterpick.newsletter.domain.MemberNewsletterStatus.*;
import static java.util.Objects.requireNonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_newsletter", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_member_newsletter_member_id_newsletter_id",
                columnNames = {"member_id", "newsletter_id"}
        )
})
public class MemberNewsletter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "newsletter_id", nullable = false)
    private Long newsletterId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private MemberNewsletterStatus status;

    private MemberNewsletter(Long memberId, Long newsletterId) {
        this.memberId = requireNonNull(memberId);
        this.newsletterId = requireNonNull(newsletterId);
        this.status = ACTIVE;
    }

    public static MemberNewsletter create(Long memberId, Long newsletterId) {
        return new MemberNewsletter(memberId, newsletterId);
    }

    public void unsubscribe() {
        if (this.status == UNSUBSCRIBED) return;
        if (this.status != ACTIVE) throw new IllegalStateException("활성 상태가 아님");

        this.status = UNSUBSCRIBED;
    }

    public void resubscribe() {
        if (this.status == ACTIVE) return;
        if (this.status != UNSUBSCRIBED) throw new IllegalStateException("구독 해지상태가 아님");

        this.status = ACTIVE;
    }

    public boolean isUnsubscribed() {
        return this.status == UNSUBSCRIBED;
    }

    public boolean isActive() {
        return this.status == ACTIVE;
    }
}
