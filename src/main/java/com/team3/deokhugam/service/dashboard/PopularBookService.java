package com.team3.deokhugam.service.dashboard;

import com.team3.deokhugam.dto.dashboard.PopularBookDto;
import com.team3.deokhugam.global.dto.CursorPageResponse;

public interface PopularBookService {

  CursorPageResponse<PopularBookDto> getPopularBooks(String period, int limit);

}
