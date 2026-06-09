package com.team3.deokhugam.repository.book;

import com.team3.deokhugam.domain.book.Book;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, UUID>, BookRepositoryCustom {

  boolean existsByIsbn(String isbn);

  Optional<Book> findByIdAndDeletedAtIsNull(UUID id);
}
