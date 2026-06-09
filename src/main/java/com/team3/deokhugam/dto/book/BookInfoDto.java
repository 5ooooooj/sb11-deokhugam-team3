package com.team3.deokhugam.dto.book;

import java.time.LocalDate;

public record BookInfoDto(
    String title,
    String author,
    String description,
    String publisher,
    LocalDate publishedDate,
    String isbn,
    String thumbnailImage
) {

}
