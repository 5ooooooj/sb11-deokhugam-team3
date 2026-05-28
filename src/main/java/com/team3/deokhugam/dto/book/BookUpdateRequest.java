package com.team3.deokhugam.dto.book;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record BookUpdateRequest(
    @NotBlank String title,
    @NotBlank String author,
    String description,
    @NotBlank String publisher,
    LocalDate publishedDate,
    String thumbnailUrl
) {
}
