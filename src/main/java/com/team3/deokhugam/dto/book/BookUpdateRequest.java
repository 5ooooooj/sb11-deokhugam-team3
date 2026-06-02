package com.team3.deokhugam.dto.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookUpdateRequest(
    @NotBlank String title,
    @NotBlank String author,
    String description,
    @NotBlank String publisher,
    @NotNull LocalDate publishedDate,
    String thumbnailUrl
) {
}
