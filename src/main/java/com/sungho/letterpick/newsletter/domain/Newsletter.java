package com.sungho.letterpick.newsletter.domain;

import com.sungho.letterpick.common.domain.BaseEntity;
import com.sungho.letterpick.common.domain.Email;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import static java.util.Objects.requireNonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "newsletter", uniqueConstraints = {
        @UniqueConstraint(name = "uk_newsletter_email", columnNames = "email")
})
public class Newsletter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 512)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private NewsletterCategory category;

    @Column(nullable = false, length = 512)
    private String subscribeUrl;

    @Column(nullable = false, length = 512)
    private String mainPageUrl;

    @Embedded
    @AttributeOverride(name = "address",
            column = @Column(name = "email", nullable = false, length = 100))
    private Email email;

    private Newsletter(String name, String description, String imageUrl,
                       NewsletterCategory category, String subscribeUrl,
                       String mainPageUrl, Email email) {
        this.name = requireNonNull(name);
        this.description = requireNonNull(description);
        this.imageUrl = requireNonNull(imageUrl);
        this.category = requireNonNull(category);
        this.subscribeUrl = requireNonNull(subscribeUrl);
        this.mainPageUrl = requireNonNull(mainPageUrl);
        this.email = requireNonNull(email);
    }

    public static Newsletter register(String name, String description, String imageUrl,
                                      NewsletterCategory category, String subscribeUrl,
                                      String mainPageUrl, Email email) {
        return new Newsletter(name, description, imageUrl, category,
                              subscribeUrl, mainPageUrl, email);
    }
}
