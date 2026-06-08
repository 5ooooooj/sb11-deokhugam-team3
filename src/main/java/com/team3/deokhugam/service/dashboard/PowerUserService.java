package com.team3.deokhugam.service.dashboard;

import com.team3.deokhugam.dto.dashboard.PowerUserDto;
import com.team3.deokhugam.global.dto.CursorPageResponse;

public interface PowerUserService {
  CursorPageResponse<PowerUserDto> getPowerUsers(String period, int limit);
}
