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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
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
    return request.hasUserId() ? review.userId.eq(request.userId()) : null;
  }

  private BooleanExpression bookIdEq(ReviewSearchRequest request) {
    return request.hasBookId() ? review.bookId.eq(request.bookId()) : null;
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
    Instant cursor = parseInstantCursor(request.cursor());

    if (request.direction().isAscending()) {
      return review.createdAt.gt(cursor);
    }
    return review.createdAt.lt(cursor);
  }

  private BooleanExpression ratingCursorCondition(ReviewSearchRequest request) {
    int cursor = parseIntCursor(request.cursor());

    if (request.direction().isAscending()) {
      return review.rating.gt(cursor)
          .or(review.rating.eq(cursor).and(review.createdAt.gt(request.after())));
    }
    return review.rating.lt(cursor)
        .or(review.rating.eq(cursor).and(review.createdAt.lt(request.after())));
  }

  private OrderSpecifier<?>[] orderSpecifiers(ReviewSearchRequest request) {
    Order direction = resolveDirection(request.direction());

    return switch (request.orderBy()) {
      case CREATED_AT -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, review.createdAt)
      };
      case RATING -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, review.rating),
          new OrderSpecifier<>(direction, review.createdAt)
      };
    };
  }

  private Order resolveDirection(Sort.Direction direction) {
    return direction.isAscending() ? Order.ASC : Order.DESC;
  }

  private Instant parseInstantCursor(String cursor) {
    try {
      return Instant.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
  }

  private int parseIntCursor(String cursor) {
    try {
      return Integer.parseInt(cursor);
    } catch (NumberFormatException e) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
  }
}