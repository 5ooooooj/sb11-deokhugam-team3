package com.team3.deokhugam.controller.review;

import com.team3.deokhugam.dto.review.ReviewCreateRequest;
import com.team3.deokhugam.dto.review.ReviewDto;
import com.team3.deokhugam.dto.review.ReviewOrderBy;
import com.team3.deokhugam.dto.review.ReviewSearchRequest;
import com.team3.deokhugam.dto.review.ReviewUpdateRequest;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "리뷰 관리", description = "리뷰 관련 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  @Operation(summary = "리뷰 등록", description = "새로운 리뷰를 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "리뷰 등록 성공",
          content = @Content(schema = @Schema(implementation = ReviewDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)"),
      @ApiResponse(responseCode = "404", description = "도서 정보 없음"),
      @ApiResponse(responseCode = "409", description = "이미 작성된 리뷰 존재"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PostMapping
  public ResponseEntity<ReviewDto> createReview(
      @Valid @RequestBody ReviewCreateRequest request) {
    ReviewDto response = reviewService.createReview(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "리뷰 상세 정보 조회", description = "리뷰 ID로 상세 정보를 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "리뷰 정보 조회 성공",
          content = @Content(schema = @Schema(implementation = ReviewDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (요청자 ID 누락)"),
      @ApiResponse(responseCode = "404", description = "리뷰 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @GetMapping("/{reviewId}")
  public ResponseEntity<ReviewDto> getReview(
      @PathVariable UUID reviewId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId) {
    ReviewDto response = reviewService.getReview(reviewId, requestUserId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "리뷰 목록 조회", description = "검색 조건에 맞는 리뷰 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "리뷰 목록 조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류, 요청자 ID 누락)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @GetMapping
  public CursorPageResponse<ReviewDto> searchReviews(
      @Parameter(description = "작성자 ID", example = "123e4567-e89b-12d3-a456-426614174000")
      @RequestParam(required = false) UUID userId,

      @Parameter(description = "도서 ID", example = "123e4567-e89b-12d3-a456-426614174000")
      @RequestParam(required = false) UUID bookId,


      @Parameter(description = "검색 키워드 (내용)", example = "재밌어요")
      @RequestParam(required = false) String keyword,


      @Parameter(description = "정렬 기준 (createdAt | rating)", example = "createdAt")
      @RequestParam(required = false, defaultValue = "createdAt") ReviewOrderBy orderBy,


      @Parameter(description = "정렬 방향 (ASC | DESC)", example = "DESC")
      @RequestParam(required = false, defaultValue = "DESC") Sort.Direction direction,


      @Parameter(description = "다음 페이지 커서 (이전 응답의 nextCursor 값을 그대로 사용)")
      @RequestParam(required = false) String cursor,


      @Parameter(description = "페이지 크기 (미지정 시 50)", example = "50")
      @RequestParam(required = false) Integer limit,


      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId) {
    ReviewSearchRequest request = ReviewSearchRequest.of(
        userId, bookId, keyword, orderBy, direction, cursor, limit, requestUserId);
    return reviewService.searchReviews(request);
  }

  @Operation(summary = "리뷰 수정", description = "본인이 작성한 리뷰를 수정합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "리뷰 수정 성공",
          content = @Content(schema = @Schema(implementation = ReviewDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)"),
      @ApiResponse(responseCode = "403", description = "리뷰 수정 권한 없음"),
      @ApiResponse(responseCode = "404", description = "리뷰 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @PatchMapping("/{reviewId}")
  public ResponseEntity<ReviewDto> updateReview(
      @PathVariable UUID reviewId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId,
      @Valid @RequestBody ReviewUpdateRequest request) {
    ReviewDto response = reviewService.updateReview(reviewId, requestUserId, request);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "리뷰 논리 삭제", description = "본인이 작성한 리뷰를 논리적으로 삭제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "리뷰 삭제 성공"),
      @ApiResponse(responseCode = "403", description = "리뷰 삭제 권한 없음"),
      @ApiResponse(responseCode = "404", description = "리뷰 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @DeleteMapping("/{reviewId}")
  public ResponseEntity<Void> deleteReview(
      @PathVariable UUID reviewId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId) {
    reviewService.deleteReview(reviewId, requestUserId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "리뷰 물리 삭제", description = "본인이 작성한 리뷰를 물리적으로 삭제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "리뷰 삭제 성공"),
      @ApiResponse(responseCode = "403", description = "리뷰 삭제 권한 없음"),
      @ApiResponse(responseCode = "404", description = "리뷰 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @DeleteMapping("/{reviewId}/hard")
  public ResponseEntity<Void> hardDeleteReview(
      @PathVariable UUID reviewId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId) {
    reviewService.hardDeleteReview(reviewId, requestUserId);
    return ResponseEntity.noContent().build();
  }
}
