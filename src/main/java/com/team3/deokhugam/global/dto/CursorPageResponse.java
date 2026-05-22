package com.team3.deokhugam.global.dto;

import java.time.Instant;
import java.util.List;

public record CursorPageResponse<T>(
    List<T> content,
    String nextcursor,
    Instant nextAfter,
    int size,
    long totalElements,
    boolean hasNext
) {}
