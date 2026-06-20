package com.sungho.letterpick.trending.ranking.adapter.webapi;

import com.sungho.letterpick.trending.ranking.adapter.webapi.dto.PublicIssueRankingsResponse;
import com.sungho.letterpick.trending.ranking.application.PublicIssueRankingWindowType;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api/v1/public-issue-rankings")
@RequiredArgsConstructor
public class PublicIssueRankingController {

    private final PublicIssueRankingFinder publicIssueRankingFinder;

    @GetMapping
    public PublicIssueRankingsResponse getRankings(
            @RequestParam PublicIssueRankingWindowType windowType,
            @RequestParam(required = false) Integer limit
    ) {
        return PublicIssueRankingsResponse.from(publicIssueRankingFinder.findTop(windowType, limit));
    }
}
