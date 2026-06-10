package com.sungho.letterpick.trending.ranking.application.provided;

import java.util.List;

public interface PublicIssueRankingFinder {

    List<PublicIssueRankingItem> findTodayTop(int limit);
}
