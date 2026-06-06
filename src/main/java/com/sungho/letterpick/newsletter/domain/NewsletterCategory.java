package com.sungho.letterpick.newsletter.domain;

public enum NewsletterCategory {

    BIZ("비즈·재테크"),
    TECH("IT·테크"),
    TREND("트렌드·라이프"),
    SOCIETY("시사·사회"),
    HOBBY("취미·자기개발"),
    TRAVEL("지역·여행"),
    CULTURE("문화·예술"),
    LIVING("리빙·인테리어"),
    AI("AI"),
    STARTUP("창업·스타트업");

    private final String label;

    NewsletterCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

}
