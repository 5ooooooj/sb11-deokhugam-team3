package com.team3.deokhugam.controller.book;

import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.service.book.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;

  @Operation(
      summary = "도서 등록",
      description = "새로운 도서를 등록합니다.",
      requestBody =
      @RequestBody(
          required = true,
          content = @Content(
              mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
              encoding = {
                  @Encoding(name = "bookData", contentType = MediaType.APPLICATION_JSON_VALUE),
                  @Encoding(name = "thumbnailImage", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
              }
          )
      )
  )
  @ApiResponse(responseCode = "201", description = "도서 등록 성공")
  @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패, ISBN 형식 오류 등)")
  @ApiResponse(responseCode = "409", description = "ISBN 중복")
  @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public BookDto create(
      @RequestPart("bookData") @Valid BookCreateRequest request,
      @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage
  ) {
    return bookService.create(request);
  }
}
