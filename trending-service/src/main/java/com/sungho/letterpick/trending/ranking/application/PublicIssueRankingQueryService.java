package com.sungho.letterpick.trending.ranking.application;

import com.sungho.letterpick.trending.publicissue.PublicIssueCandidateStatus;
import com.sungho.letterpick.trending.ranking.adapter.persistence.PublicIssueRankingQueryRepository;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingFinder;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingItem;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PublicIssueRankingQueryService implements PublicIssueRankingFinder {

    private static final ZoneId RANKING_ZONE = ZoneId.of("Asia/Seoul");

    private final PublicIssueRankingQueryRepository rankingQueryRepository;
    private final Clock clock;

    @Override
    public List<PublicIssueRankingItem> findTodayTop(int limit) {
        PublicIssueRankingLimit.validate(limit);

        LocalDate today = LocalDate.now(clock.withZone(RANKING_ZONE));
        Instant windowStart = today.atStartOfDay(RANKING_ZONE).toInstant();
        Instant windowEnd = today.plusDays(1).atStartOfDay(RANKING_ZONE).toInstant();

        return rankingQueryRepository.findTopByWindow(
                PublicIssueCandidateStatus.AVAILABLE,
                windowStart,
                windowEnd,
                PageRequest.of(0, limit)
        );
    }
}
