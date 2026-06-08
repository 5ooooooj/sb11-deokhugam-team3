package com.team3.deokhugam.repository.review;

import static com.team3.deokhugam.domain.review.QReview.review;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.dto.review.ReviewSearchRequest;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewRepositoryCustomImpl implements ReviewRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  public ReviewRepositoryCustomImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  @Override
  public List<Review> search(ReviewSearchRequest request) {
    return queryFactory
        .selectFrom(review)
        .join(review.user).fetchJoin()
        .join(review.book).fetchJoin()
        .where(
            notDeleted(),
            userIdEq(request),
            bookIdEq(request),
            containsKeyword(request),
            cursorCondition(request)
        )
        .orderBy(orderSpecifiers(request))
        .limit(request.limit())
        .fetch();
  }

  @Override
  public long count(ReviewSearchRequest request) {
    Long count = queryFactory
        .select(review.count())
        .from(review)
        .where(
            notDeleted(),
            userIdEq(request),
            bookIdEq(request),
            containsKeyword(request)
        )
        .fetchOne();

    return count == null ? 0 : count;
  }

  private BooleanExpression notDeleted() {
    return review.deletedAt.isNull();
  }

  private BooleanExpression userIdEq(ReviewSearchRequest request) {
    return request.hasUserId() ? review.user.id.eq(request.userId()) : null;
  }

  private BooleanExpression bookIdEq(ReviewSearchRequest request) {
    return request.hasBookId() ? review.book.id.eq(request.bookId()) : null;
  }

  private BooleanExpression containsKeyword(ReviewSearchRequest request) {
    if (!request.hasKeyword()) {
      return null;
    }
    return review.content.containsIgnoreCase(request.keyword());
  }

  private BooleanExpression cursorCondition(ReviewSearchRequest request) {
    if (!request.hasCursor()) {
      return null;
    }

    return switch (request.orderBy()) {
      case CREATED_AT -> createdAtCursorCondition(request);
      case RATING -> ratingCursorCondition(request);
    };
  }

  private BooleanExpression createdAtCursorCondition(ReviewSearchRequest request) {
    String[] parts = decodeCursor(request.cursor());
    if (parts.length != 2) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
    Instant createdAt = parseInstant(parts[0]);
    UUID id = parseUuid(parts[1]);
    boolean asc = request.direction().isAscending();

    BooleanExpression sameCreatedAtTieBreak = review.createdAt.eq(createdAt)
        .and(asc ? review.id.gt(id) : review.id.lt(id));
    BooleanExpression differentCreatedAt =
        asc ? review.createdAt.gt(createdAt) : review.createdAt.lt(createdAt);

    return differentCreatedAt.or(sameCreatedAtTieBreak);
  }

  private BooleanExpression ratingCursorCondition(ReviewSearchRequest request) {
    String[] parts = decodeCursor(request.cursor());
    if (parts.length != 3) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
    int rating = parseInt(parts[0]);
    Instant createdAt = parseInstant(parts[1]);
    UUID id = parseUuid(parts[2]);
    boolean asc = request.direction().isAscending();

    BooleanExpression differentRating =
        asc ? review.rating.gt(rating) : review.rating.lt(rating);
    BooleanExpression sameRatingDifferentCreatedAt = review.rating.eq(rating)
        .and(asc ? review.createdAt.gt(createdAt) : review.createdAt.lt(createdAt));
    BooleanExpression sameRatingSameCreatedAtTieBreak = review.rating.eq(rating)
        .and(review.createdAt.eq(createdAt))
        .and(asc ? review.id.gt(id) : review.id.lt(id));

    return differentRating
        .or(sameRatingDifferentCreatedAt)
        .or(sameRatingSameCreatedAtTieBreak);
  }

  private OrderSpecifier<?>[] orderSpecifiers(ReviewSearchRequest request) {
    Order direction = resolveDirection(request.direction());

    return switch (request.orderBy()) {
      case CREATED_AT -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, review.createdAt),
          new OrderSpecifier<>(direction, review.id)
      };
      case RATING -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, review.rating),
          new OrderSpecifier<>(direction, review.createdAt),
          new OrderSpecifier<>(direction, review.id)
      };
    };
  }

  private Order resolveDirection(Sort.Direction direction) {
    return direction.isAscending() ? Order.ASC : Order.DESC;
  }

  private String[] decodeCursor(String cursor) {
    try {
      byte[] decoded = Base64.getUrlDecoder().decode(cursor);
      return new String(decoded, StandardCharsets.UTF_8).split("\\|");
    } catch (IllegalArgumentException e) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
  }

  private Instant parseInstant(String value) {
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException e) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
  }

  private int parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
  }

  private UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
  }
}