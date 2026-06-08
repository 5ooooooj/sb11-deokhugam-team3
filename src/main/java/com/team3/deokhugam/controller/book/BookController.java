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
import com.team3.deokhugam.dto.book.BookOrderBy;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.dto.dashboard.PopularBookDto;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.book.BookService;
import com.team3.deokhugam.service.dashboard.PopularBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "도서 관리", description = "도서 관련 API")
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;
  private final PopularBookService popularBookService;

  @BookCreateApi
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BookDto create(
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId,
      @Valid @RequestPart("bookData") BookCreateRequest request,
      @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage
  ) {
    return bookService.create(requestUserId, request, thumbnailImage);
  }

  @BookSearchApi
  @GetMapping
  public CursorPageResponse<BookDto> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) BookOrderBy orderBy,
      @RequestParam(required = false) Sort.Direction direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "50") Integer limit
  ) {
    BookSearchRequest request =
        BookSearchRequest.of(
            keyword,
            orderBy,
            direction,
            cursor,
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
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId,
      @Valid @RequestPart("bookData") BookUpdateRequest request,
      @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage
  ) {
    return bookService.update(bookId, requestUserId, request, thumbnailImage);
  }

  @BookDeleteApi
  @DeleteMapping("/{bookId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @Parameter(description = "삭제할 도서 ID")
      @PathVariable UUID bookId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId
  ) {
    bookService.delete(bookId, requestUserId);
  }

  @BookHardDeleteApi
  @DeleteMapping("/{bookId}/hard")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void hardDelete(
      @Parameter(description = "물리 삭제 도서 ID")
      @PathVariable UUID bookId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId
  ) {
    bookService.hardDelete(bookId, requestUserId);
  }

  @Operation(summary = "인기 도서 목록 조회", description = "기간별 인기 도서 목록을 조회합니다")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "인기 도서 목록 조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (랭킹 기간 오류)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @GetMapping("/popular")
  public ResponseEntity<CursorPageResponse<PopularBookDto>> getPopularBooks(
      @RequestParam(defaultValue = "DAILY") String period,
      // 프로토타입엔 정렬, 페이지네이션이 없지만 api 명세서 기준으로 있으므로 파라미터는 받괴 실제 사용 x, 여유되면 추후 구현
      @RequestParam(defaultValue = "ASC") String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam(defaultValue = "50") int limit
  ) {
    CursorPageResponse<PopularBookDto> response =
        popularBookService.getPopularBooks(period, limit);
    return ResponseEntity.ok(response);
  }
}