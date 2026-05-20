CREATE TABLE member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    social_provider VARCHAR(20) NOT NULL,
    social_provider_id VARCHAR(100) NOT NULL,
    nickname VARCHAR(20) NOT NULL,
    newsletter_inbox_address VARCHAR(254) NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_email UNIQUE (email),
    CONSTRAINT uk_member_nickname UNIQUE (nickname),
    CONSTRAINT uk_member_newsletter_inbox_address UNIQUE (newsletter_inbox_address),
    CONSTRAINT uk_member_social_identity UNIQUE (social_provider, social_provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE newsletter (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    category VARCHAR(20) NOT NULL,
    subscribe_url VARCHAR(512) NOT NULL,
    main_page_url VARCHAR(512) NOT NULL,
    email VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_newsletter_email UNIQUE (email),
    INDEX idx_newsletter_category_id (category, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE member_newsletter (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    newsletter_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_newsletter_member_id_newsletter_id UNIQUE (member_id, newsletter_id),
    INDEX idx_member_newsletter_newsletter_id (newsletter_id),
    CONSTRAINT fk_member_newsletter_member
        FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_member_newsletter_newsletter
        FOREIGN KEY (newsletter_id) REFERENCES newsletter (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE inbound_email (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_key VARCHAR(255) NOT NULL,
    raw_reference VARCHAR(512) NOT NULL,
    recipient_address VARCHAR(320) NOT NULL,
    sender_email VARCHAR(320) NOT NULL,
    subject VARCHAR(512) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    status VARCHAR(30) NOT NULL,
    member_id BIGINT NULL,
    newsletter_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_inbound_email_message_key UNIQUE (message_key),
    INDEX idx_inbound_email_member_id (member_id),
    INDEX idx_inbound_email_newsletter_id (newsletter_id),
    CONSTRAINT fk_inbound_email_member
        FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_inbound_email_newsletter
        FOREIGN KEY (newsletter_id) REFERENCES newsletter (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE newsletter_issue (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    newsletter_id BIGINT NOT NULL,
    inbound_email_id BIGINT NOT NULL,
    subject VARCHAR(512) NOT NULL,
    content LONGTEXT NOT NULL,
    preview_text VARCHAR(120) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    read_status BIT(1) NOT NULL,
    deleted BIT(1) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_newsletter_issue_inbound_email_id UNIQUE (inbound_email_id),
    INDEX idx_newsletter_issue_newsletter_id (newsletter_id),
    INDEX idx_newsletter_issue_member_deleted_received_id (member_id, deleted, received_at, id),
    CONSTRAINT fk_newsletter_issue_member
        FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_newsletter_issue_newsletter
        FOREIGN KEY (newsletter_id) REFERENCES newsletter (id),
    CONSTRAINT fk_newsletter_issue_inbound_email
        FOREIGN KEY (inbound_email_id) REFERENCES inbound_email (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
