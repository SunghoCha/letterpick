package com.sungho.letterpick.trending.ranking.adapter.persistence;

import java.io.Serializable;
import java.util.Objects;

public class PublicIssueRankingSummaryId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String windowType;
    private String windowKey;
    private Long issueId;

    public PublicIssueRankingSummaryId() {
    }

    public PublicIssueRankingSummaryId(String windowType, String windowKey, Long issueId) {
        this.windowType = windowType;
        this.windowKey = windowKey;
        this.issueId = issueId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PublicIssueRankingSummaryId that)) {
            return false;
        }
        return Objects.equals(windowType, that.windowType)
                && Objects.equals(windowKey, that.windowKey)
                && Objects.equals(issueId, that.issueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(windowType, windowKey, issueId);
    }
}
