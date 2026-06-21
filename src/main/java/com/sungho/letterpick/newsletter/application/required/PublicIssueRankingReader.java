package com.sungho.letterpick.newsletter.application.required;

import com.sungho.letterpick.newsletter.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.newsletter.application.provided.PublicIssueRankingWindowType;

import java.util.List;

public interface PublicIssueRankingReader {

    List<PublicIssueRankingItem> findTop(PublicIssueRankingWindowType windowType, int limit);
}
