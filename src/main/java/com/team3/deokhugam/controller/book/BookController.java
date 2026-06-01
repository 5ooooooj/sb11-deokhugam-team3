package com.team3.deokhugam.controller.book;

import com.team3.deokhugam.controller.book.docs.BookCreateApi;
import com.team3.deokhugam.controller.book.docs.BookSearchApi;
import com.team3.deokhugam.controller.book.docs.BookFindByIdApi;
import com.team3.deokhugam.controller.book.docs.BookUpdateApi;
import com.team3.deokhugam.controller.book.docs.BookDeleteApi;
import com.team3.deokhugam.controller.book.docs.BookHardDeleteApi;
import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookUpdateRequest;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.book.BookService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "도서 관리", description = "도서 관련 API")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;

  @BookCreateApi
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BookDto create(
      @Valid @RequestPart("bookData") BookCreateRequest request,
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

  @BookUpdateApi
  @PatchMapping(
      value = "/{bookId}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public BookDto update(
      @Parameter(description = "수정할 도서 ID")
      @PathVariable UUID bookId,
      @Valid @RequestPart("bookData") BookUpdateRequest request,
      @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage
  ) {
    return bookService.update(bookId, request);
  }

  @BookDeleteApi
  @DeleteMapping("/{bookId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @Parameter(description = "삭제할 도서 ID")
      @PathVariable UUID bookId
  ) {
    bookService.delete(bookId);
  }

  @BookHardDeleteApi
  @DeleteMapping("/{bookId}/hard")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void hardDelete(
      @Parameter(description = "물리 삭제 도서 ID")
      @PathVariable UUID bookId
  ) {
    bookService.hardDelete(bookId);
  }
}