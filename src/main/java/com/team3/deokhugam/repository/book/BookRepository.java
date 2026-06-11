package com.team3.deokhugam.repository.book;

import com.team3.deokhugam.domain.book.Book;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, UUID>, BookRepositoryCustom {

  boolean existsByIsbn(String isbn);

  Optional<Book> findByIdAndDeletedAtIsNull(UUID id);

  @Modifying
  @Query("UPDATE Book b SET b.rating = :rating, b.reviewCount = :reviewCount WHERE b.id = :bookId")
  void updateRatingStats(@Param("bookId") UUID bookId,
      @Param("rating") BigDecimal rating,
      @Param("reviewCount") int reviewCount);
}