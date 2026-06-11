package com.sungho.letterpick.trending.ranking.adapter.webapi;

import com.sungho.letterpick.trending.ranking.adapter.webapi.dto.PublicIssueRankingsResponse;

public interface PublicIssueRankingControllerApi {

    PublicIssueRankingsResponse getTodayRankings(int limit);
}
