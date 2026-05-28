package com.team3.deokhugam.domain.book;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public class BookTestFactory {

  private String title = "테스트 도서";
  private String author = "테스트 저자";
  private String description = "테스트 설명";
  private String publisher = "테스트 출판사";
  private LocalDate publishedDate = LocalDate.of(2026, 1, 1);
  private String isbn = "9780000000000";
  private String thumbnailUrl = "https://example.com/book.jpg";
  private BigDecimal rating;
  private Integer reviewCount;
  private UUID id;
  private Instant createdAt;

  private BookTestFactory() {
  }

  public static BookTestFactory book() {
    return new BookTestFactory();
  }

  public BookTestFactory title(String title) {
    this.title = title;
    return this;
  }

  public BookTestFactory author(String author) {
    this.author = author;
    return this;
  }

  public BookTestFactory description(String description) {
    this.description = description;
    return this;
  }

  public BookTestFactory publisher(String publisher) {
    this.publisher = publisher;
    return this;
  }

  public BookTestFactory publishedDate(LocalDate publishedDate) {
    this.publishedDate = publishedDate;
    return this;
  }

  public BookTestFactory isbn(String isbn) {
    this.isbn = isbn;
    return this;
  }

  public BookTestFactory thumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
    return this;
  }

  public BookTestFactory rating(BigDecimal rating) {
    this.rating = rating;
    return this;
  }

  public BookTestFactory reviewCount(int reviewCount) {
    this.reviewCount = reviewCount;
    return this;
  }

  public BookTestFactory id(UUID id) {
    this.id = id;
    return this;
  }

  public BookTestFactory createdAt(Instant createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Book build() {
    Book book = new Book(
        title,
        author,
        description,
        publisher,
        publishedDate,
        isbn,
        thumbnailUrl
    );

    if (rating != null) {
      ReflectionTestUtils.setField(book, "rating", rating);
    }

    if (reviewCount != null) {
      ReflectionTestUtils.setField(book, "reviewCount", reviewCount);
    }

    if (id != null) {
      ReflectionTestUtils.setField(book, "id", id);
    }

    if (createdAt != null) {
      ReflectionTestUtils.setField(book, "createdAt", createdAt);
      ReflectionTestUtils.setField(book, "updatedAt", createdAt);
    }

    return book;
  }
}