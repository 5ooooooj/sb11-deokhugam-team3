package com.team3.deokhugam.repository.book;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookOrderBy;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.exception.book.InvalidBookSearchConditionException;
import io.swagger.v3.oas.annotations.media.Encoding;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookRepositoryCustomImpl implements BookRepositoryCustom {

  private final EntityManager entityManager;

  @Override
  public List<Book> search(BookSearchRequest request) {
    StringBuilder jpql =
        new StringBuilder("select b from Book b where b.deletedAt is null");
    Map<String, Object> parameters = new HashMap<>();

    appendKeywordCondition(jpql, parameters, request);
    appendCursorCondition(jpql, parameters, request);

    jpql.append(" order by ")
        .append(resolveOrderProperty(request.orderBy()))
        .append(" ")
        .append(resolveDirection(request.direction()))
        .append(", b.createdAt ")
        .append(resolveDirection(request.direction()));

    TypedQuery<Book> query = entityManager.createQuery(jpql.toString(), Book.class);
    parameters.forEach(query::setParameter);

    return query
        .setMaxResults(request.limit())
        .getResultList();
  }

  @Override
  public long count(BookSearchRequest request) {
    StringBuilder jpql =
        new StringBuilder("select count(b) from Book b where b.deletedAt is null");
    Map<String, Object> parameters = new HashMap<>();

    appendKeywordCondition(jpql, parameters, request);

    TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class);
    parameters.forEach(query::setParameter);

    return query.getSingleResult();
  }

  private void appendKeywordCondition(
      StringBuilder jpql,
      Map<String, Object> parameters,
      BookSearchRequest request
  ) {
    if (!request.hasKeyword()) {
      return;
    }

    jpql.append("""
          and (
            lower(b.title) like lower(:keyword)
            or lower(b.author) like lower(:keyword)
            or lower(b.isbn) like lower(:keyword)
          )
        """
    );
    parameters.put("keyword", "%" + request.keyword() + "%");
  }

  private void appendCursorCondition(
      StringBuilder jpql,
      Map<String, Object> parameters,
      BookSearchRequest request
  ) {
    if (!request.hasCursor()) {
      return;
    }

    String orderProperty = resolveOrderProperty(request.orderBy());
    String operator = request.direction().isAscending() ? ">" : "<";

    jpql.append(" and (")
        .append(orderProperty)
        .append(" ")
        .append(operator)
        .append(" :cursor")
        .append(" or (")
        .append(orderProperty)
        .append(" = :cursor")
        .append(" and b.createdAt ")
        .append(operator)
        .append(" :after")
        .append("))");

    parameters.put("cursor", parseCursor(request));
    parameters.put("after", request.after());
  }

  private Object parseCursor(BookSearchRequest request) {
    try {
      return switch (request.orderBy()) {
        case TITLE -> request.cursor();
        case PUBLISHED_DATE -> LocalDate.parse(request.cursor());
        case RATING -> new BigDecimal(request.cursor());
        case REVIEW_COUNT -> Integer.parseInt(request.cursor());
      };
    } catch (NumberFormatException | DateTimeParseException e) {
      throw new InvalidBookSearchConditionException("잘못된 커서 값입니다.");
    }
  }

    private String resolveOrderProperty (BookOrderBy orderBy){
      return switch (orderBy) {
        case TITLE -> "b.title";
        case PUBLISHED_DATE -> "b.publishedDate";
        case RATING -> "b.rating";
        case REVIEW_COUNT -> "b.reviewCount";
      };
    }

    private String resolveDirection (Sort.Direction direction){
      return direction.isAscending() ? "ASC" : "DESC";
    }
  }
