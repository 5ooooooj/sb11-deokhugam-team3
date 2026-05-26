package com.team3.deokhugam.controller.book;

import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.service.book.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BookDto create(@RequestPart("bookData") @Valid BookCreateRequest request){
    return bookService.create(request);
  }
}
