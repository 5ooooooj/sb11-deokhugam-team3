package com.team3.deokhugam.service.dashboard;

import com.team3.deokhugam.dto.dashboard.PopularReviewDto;
import com.team3.deokhugam.global.dto.CursorPageResponse;

public interface PopularReviewService {

  CursorPageResponse<PopularReviewDto> getPopularReviews(String period, int limit);

}
