package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingFinder;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingLimit;
import com.sungho.letterpick.trending.ranking.application.required.PublicIssueRankingReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicIssueRankingQueryService implements PublicIssueRankingFinder {

    private static final ZoneId RANKING_ZONE = ZoneId.of("Asia/Seoul");

    private final PublicIssueRankingReader rankingReader;
    private final Clock clock;

    @Override
    public List<PublicIssueRankingItem> findTodayTop(int limit) {
        PublicIssueRankingLimit.validate(limit);

        LocalDate today = LocalDate.now(clock.withZone(RANKING_ZONE));
        return rankingReader.findTop(PublicIssueRankingWindow.daily(today, RANKING_ZONE), limit);
    }
}
