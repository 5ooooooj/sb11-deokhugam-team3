package com.team3.deokhugam.repository.book;

import static com.team3.deokhugam.domain.book.QBook.book;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team3.deokhugam.domain.book.Book;
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
            containsKeyword(request),
            cursorCondition(request)
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
    String cursor = request.cursor();

    if (request.direction().isAscending()) {
      return book.title.gt(cursor)
          .or(book.title.eq(cursor).and(book.createdAt.gt(request.after())));
    }

    return book.title.lt(cursor)
        .or(book.title.eq(cursor).and(book.createdAt.lt(request.after())));
  }

  private BooleanExpression publishedDateCursorCondition(BookSearchRequest request) {
    LocalDate cursor = parsePublishedDateCursor(request.cursor());

    if (request.direction().isAscending()) {
      return book.publishedDate.gt(cursor)
          .or(book.publishedDate.eq(cursor).and(book.createdAt.gt(request.after())));
    }

    return book.publishedDate.lt(cursor)
        .or(book.publishedDate.eq(cursor).and(book.createdAt.lt(request.after())));
  }

  private BooleanExpression ratingCursorCondition(BookSearchRequest request) {
    BigDecimal cursor = parseRatingCursor(request.cursor());

    if (request.direction().isAscending()) {
      return book.rating.gt(cursor)
          .or(book.rating.eq(cursor).and(book.createdAt.gt(request.after())));
    }
    return book.rating.lt(cursor)
        .or(book.rating.eq(cursor).and(book.createdAt.lt(request.after())));
  }

  private BooleanExpression reviewCountCursorCondition(BookSearchRequest request) {
    int cursor = parseReviewCountCursor(request.cursor());

    if (request.direction().isAscending()) {
      return book.reviewCount.gt(cursor)
          .or(book.reviewCount.eq(cursor).and(book.createdAt.gt(request.after())));
    }

    return book.reviewCount.lt(cursor)
        .or(book.reviewCount.eq(cursor).and(book.createdAt.lt(request.after())));
  }


  private OrderSpecifier<?>[] orderSpecifiers(BookSearchRequest request) {
    Order direction = resolveDirection(request.direction());

    return switch (request.orderBy()) {
      case TITLE -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, book.title),
          new OrderSpecifier<>(direction, book.createdAt)
      };
      case PUBLISHED_DATE -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, book.publishedDate),
          new OrderSpecifier<>(direction, book.createdAt)
      };
      case RATING -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, book.rating),
          new OrderSpecifier<>(direction, book.createdAt)
      };
      case REVIEW_COUNT -> new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, book.reviewCount),
          new OrderSpecifier<>(direction, book.createdAt)
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
      throw new InvalidBookSearchConditionException("잘못된 커서 값입니다.");
    }
  }


  private BigDecimal parseRatingCursor(String cursor) {
    try {
      return new BigDecimal(cursor);
    } catch (NumberFormatException e) {
      throw new InvalidBookSearchConditionException("잘못된 커서 값입니다.");
    }
  }


  private int parseReviewCountCursor(String cursor) {
    try {
      return Integer.parseInt(cursor);
    } catch (NumberFormatException e) {
      throw new InvalidBookSearchConditionException("잘못된 커서 값입니다.");
    }
  }

}