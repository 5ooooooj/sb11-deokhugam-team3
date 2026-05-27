package com.team3.deokhugam.repository.book;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import java.util.List;

public interface BookRepositoryCustom {

  List<Book> search(BookSearchRequest request);
}
