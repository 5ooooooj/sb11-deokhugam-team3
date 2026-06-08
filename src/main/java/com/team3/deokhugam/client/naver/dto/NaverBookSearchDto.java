package com.team3.deokhugam.client.naver.dto;

import java.util.List;

public record NaverBookSearchDto(
    String lastBuildDate,
    int total,
    int start,
    int display,
    List<NaverBookItemDto> items
) {

  public boolean hasNoItems() {
    return items == null || items.isEmpty();
  }
}
