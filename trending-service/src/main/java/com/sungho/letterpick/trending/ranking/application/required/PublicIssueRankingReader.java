package com.sungho.letterpick.trending.ranking.application.required;

import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindow;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;

import java.util.List;

public interface PublicIssueRankingReader {

    List<PublicIssueRankingItem> findTop(PublicIssueRankingWindow window, int limit);
}
