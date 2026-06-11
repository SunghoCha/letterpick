package com.sungho.letterpick.trending.ranking.adapter.webapi;

import com.sungho.letterpick.trending.ranking.adapter.webapi.dto.PublicIssueRankingsResponse;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingFinder;
import com.sungho.letterpick.trending.ranking.application.provided.PublicIssueRankingLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/api/v1/public-issue-rankings")
@RequiredArgsConstructor
public class PublicIssueRankingController implements PublicIssueRankingControllerApi {

    private final PublicIssueRankingFinder publicIssueRankingFinder;

    @Override
    @GetMapping("/today")
    public PublicIssueRankingsResponse getTodayRankings(
            @RequestParam(defaultValue = PublicIssueRankingLimit.DEFAULT_VALUE) int limit
    ) {
        validateLimit(limit);
        return PublicIssueRankingsResponse.from(publicIssueRankingFinder.findTodayTop(limit));
    }

    private void validateLimit(int limit) {
        if (!PublicIssueRankingLimit.isInRange(limit)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    PublicIssueRankingLimit.errorMessage()
            );
        }
    }
}
