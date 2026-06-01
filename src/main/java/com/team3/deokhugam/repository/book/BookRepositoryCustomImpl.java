package com.team3.deokhugam.repository.book;

import static com.team3.deokhugam.domain.book.QBook.book;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookCursor;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.exception.book.InvalidBookSearchConditionException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class BookRepositoryCustomImpl implements BookRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  public BookRepositoryCustomImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  @Override
  public List<Book> search(BookSearchRequest request) {
    return queryFactory
        .selectFrom(book)
        .where(
            notDeleted(),
            containsKeyword(request),
            cursorCondition(request)
        )
        .orderBy(orderSpecifiers(request))
        .limit(request.limit())
        .fetch();
  }

  @Override
  public long count(BookSearchRequest request) {
    Long count = queryFactory
        .select(book.count())
        .from(book)
        .where(
            notDeleted(),
            containsKeyword(request)
        )
        .fetchOne();

    return count == null ? 0 : count;
  }

  private BooleanExpression notDeleted() {
    return book.deletedAt.isNull();
  }

  private BooleanExpression containsKeyword(BookSearchRequest request) {
    if (!request.hasKeyword()) {
      return null;
    }

    return book.title.containsIgnoreCase(request.keyword())
        .or(book.author.containsIgnoreCase(request.keyword()))
        .or(book.isbn.containsIgnoreCase(request.keyword()));
  }

  private BooleanExpression cursorCondition(BookSearchRequest request) {
    if (!request.hasCursor()) {
      return null;
    }

    return switch (request.orderBy()) {
      case TITLE -> titleCursorCondition(request);
      case PUBLISHED_DATE -> publishedDateCursorCondition(request);
      case RATING -> ratingCursorCondition(request);
      case REVIEW_COUNT -> reviewCountCursorCondition(request);
    };
  }

  private BooleanExpression titleCursorCondition(BookSearchRequest request) {
    BookCursor cursor = request.cursor();
    String value = cursor.value();

    if (request.direction().isAscending()) {
      return book.title.gt(value)
          .or(book.title.eq(value).and(book.createdAt.gt(cursor.createdAt())))
          .or(book.title.eq(value)
              .and(book.createdAt.eq(cursor.createdAt()))
              .and(book.id.gt(cursor.id())));
    }

    return book.title.lt(value)
        .or(book.title.eq(value).and(book.createdAt.lt(cursor.createdAt())))
        .or(book.title.eq(value)
            .and(book.createdAt.eq(cursor.createdAt()))
            .and(book.id.lt(cursor.id())));
  }

  private BooleanExpression publishedDateCursorCondition(BookSearchRequest request) {
    BookCursor cursor = request.cursor();
    LocalDate value = parsePublishedDateCursor(cursor.value());

    if (request.direction().isAscending()) {
      return book.publishedDate.gt(value)
          .or(book.publishedDate.eq(value).and(book.createdAt.gt(cursor.createdAt())))
          .or(book.publishedDate.eq(value)
              .and(book.createdAt.eq(cursor.createdAt()))
              .and(book.id.gt(cursor.id())));
    }

    return book.publishedDate.lt(value)
        .or(book.publishedDate.eq(value).and(book.createdAt.lt(cursor.createdAt())))
        .or(book.publishedDate.eq(value)
            .and(book.createdAt.eq(cursor.createdAt()))
            .and(book.id.lt(cursor.id())));
  }

  private BooleanExpression ratingCursorCondition(BookSearchRequest request) {
    BookCursor cursor = request.cursor();
    BigDecimal value = parseRatingCursor(cursor.value());

    if (request.direction().isAscending()) {
      return book.rating.gt(value)
          .or(book.rating.eq(value).and(book.createdAt.gt(cursor.createdAt())))
          .or(book.rating.eq(value)
              .and(book.createdAt.eq(cursor.createdAt()))
              .and(book.id.gt(cursor.id())));
    }

    return book.rating.lt(value)
        .or(book.rating.eq(value).and(book.createdAt.lt(cursor.createdAt())))
        .or(book.rating.eq(value)
            .and(book.createdAt.eq(cursor.createdAt()))
            .and(book.id.lt(cursor.id())));
  }

  private BooleanExpression reviewCountCursorCondition(BookSearchRequest request) {
    BookCursor cursor = request.cursor();
    int value = parseReviewCountCursor(cursor.value());

    if (request.direction().isAscending()) {
      return book.reviewCount.gt(value)
          .or(book.reviewCount.eq(value).and(book.createdAt.gt(cursor.createdAt())))
          .or(book.reviewCount.eq(value)
              .and(book.createdAt.eq(cursor.createdAt()))
              .and(book.id.gt(cursor.id())));
    }

    return book.reviewCount.lt(value)
        .or(book.reviewCount.eq(value).and(book.createdAt.lt(cursor.createdAt())))
        .or(book.reviewCount.eq(value)
            .and(book.createdAt.eq(cursor.createdAt()))
            .and(book.id.lt(cursor.id())));
  }

  private OrderSpecifier<?>[] orderSpecifiers(BookSearchRequest request) {
    Order direction = resolveDirection(request.direction());

    return switch (request.orderBy()) {
      case TITLE -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, book.title),
          new OrderSpecifier<>(direction, book.createdAt),
          new OrderSpecifier<>(direction, book.id)
      };
      case PUBLISHED_DATE -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, book.publishedDate),
          new OrderSpecifier<>(direction, book.createdAt),
          new OrderSpecifier<>(direction, book.id)
      };
      case RATING -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, book.rating),
          new OrderSpecifier<>(direction, book.createdAt),
          new OrderSpecifier<>(direction, book.id)
      };
      case REVIEW_COUNT -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, book.reviewCount),
          new OrderSpecifier<>(direction, book.createdAt),
          new OrderSpecifier<>(direction, book.id)
      };
    };
  }

  private Order resolveDirection(Sort.Direction direction) {
    return direction.isAscending() ? Order.ASC : Order.DESC;
  }

  private LocalDate parsePublishedDateCursor(String cursor) {
    try {
      return LocalDate.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new InvalidBookSearchConditionException();
    }
  }

  private BigDecimal parseRatingCursor(String cursor) {
    try {
      return new BigDecimal(cursor);
    } catch (NumberFormatException e) {
      throw new InvalidBookSearchConditionException();
    }
  }

  private int parseReviewCountCursor(String cursor) {
    try {
      return Integer.parseInt(cursor);
    } catch (NumberFormatException e) {
      throw new InvalidBookSearchConditionException();
    }
  }
}