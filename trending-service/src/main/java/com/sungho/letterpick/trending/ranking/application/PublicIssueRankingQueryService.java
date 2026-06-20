package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingFinder;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingLimitPolicy;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PublicIssueRankingQueryService implements PublicIssueRankingFinder {

    private final PublicIssueRankingReader rankingReader;
    private final PublicIssueRankingLimitPolicy limitPolicy;
    private final PublicIssueRankingWindowCalculator windowCalculator;

    public PublicIssueRankingQueryService(
            PublicIssueRankingReader rankingReader,
            PublicIssueRankingLimitPolicy limitPolicy,
            PublicIssueRankingWindowCalculator windowCalculator
    ) {
        this.rankingReader = rankingReader;
        this.limitPolicy = limitPolicy;
        this.windowCalculator = windowCalculator;
    }

    @Override
    public List<PublicIssueRankingItem> findTop(PublicIssueRankingWindowType windowType, Integer limit) {
        int resolvedLimit = limitPolicy.resolve(limit);

        return rankingReader.findTop(windowCalculator.currentWindow(windowType), resolvedLimit);
    }
}
