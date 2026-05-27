package com.team3.deokhugam.controller.book;

import com.team3.deokhugam.controller.book.docs.BookCreateApi;
import com.team3.deokhugam.controller.book.docs.BookSearchApi;
import com.team3.deokhugam.controller.book.docs.BookFindByIdApi;
import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.book.BookService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;

  @BookCreateApi
  @PostMapping(
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseStatus(HttpStatus.CREATED)
  public BookDto create(
      @RequestPart("bookData") @Valid BookCreateRequest request,
      @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage
  ) {
    return bookService.create(request);
  }

  @BookSearchApi
  @GetMapping
  public CursorPageResponse<BookDto> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String orderBy,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant after,
      @RequestParam(defaultValue = "50") Integer limit
  ) {
    BookSearchRequest request =
        BookSearchRequest.of(
            keyword,
            orderBy,
            direction,
            cursor,
            after,
            limit
        );

    return bookService.search(request);
  }

  @BookFindByIdApi
  @GetMapping("/{bookId}")
  public BookDto findById(@PathVariable UUID bookId) {
    return bookService.findById(bookId);
  }
}