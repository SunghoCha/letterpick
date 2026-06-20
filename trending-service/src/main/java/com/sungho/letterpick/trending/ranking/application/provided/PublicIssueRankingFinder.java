package com.sungho.letterpick.trending.ranking.application.provided;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindowType;

import java.util.List;

public interface PublicIssueRankingFinder {

    List<PublicIssueRankingItem> findTop(PublicIssueRankingWindowType windowType, Integer limit);
}
