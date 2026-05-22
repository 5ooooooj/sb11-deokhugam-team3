package com.team3.deokhugam.dto.book;

import com.team3.deokhugam.domain.book.Book;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookDto(
    UUID id,
    String title,
    String author,
    String description,
    String publisher,
    LocalDate publishedDate,
    String isbn,
    String thumbnailUrl,
    int reviewCount,
    BigDecimal rating
) {

  public static BookDto from(Book book) {
    return new BookDto(
        book.getId(),
        book.getTitle(),
        book.getAuthor(),
        book.getDescription(),
        book.getPublisher(),
        book.getPublishedDate(),
        book.getIsbn(),
        book.getThumbnailUrl(),
        book.getReviewCount(),
        book.getRating()
    );
  }
}
