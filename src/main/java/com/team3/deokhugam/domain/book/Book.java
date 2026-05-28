package com.team3.deokhugam.domain.book;

import com.team3.deokhugam.domain.base.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "books")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends SoftDeletableEntity {

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String author;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private String publisher;

  @Column(name = "published_date", nullable = false)
  private LocalDate publishedDate;

  @Column(unique = true)
  private String isbn;

  @Column(name = "thumbnail_url")
  private String thumbnailUrl;

  @Column(name = "review_count", nullable = false)
  private int reviewCount = 0;

  @Column(nullable = false, precision = 3, scale = 2)
  private BigDecimal rating = BigDecimal.ZERO;

  public Book(
      String title,
      String author,
      String description,
      String publisher,
      LocalDate publishedDate,
      String isbn,
      String thumbnailUrl
  ) {
    this.title = title;
    this.author = author;
    this.description = description;
    this.publisher = publisher;
    this.publishedDate = publishedDate;
    this.isbn = isbn;
    this.thumbnailUrl = thumbnailUrl;
  }

  public void update(
      String title,
      String author,
      String description,
      String publisher,
      LocalDate publishedDate,
      String thumbnailUrl
  ) {
        this.title = title;
        this.author = author;
        this.description = description;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.thumbnailUrl = thumbnailUrl;
  }
}
