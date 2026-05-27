package com.team3.deokhugam.repository.book;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookOrderBy;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import io.swagger.v3.oas.annotations.media.Encoding;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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

  private String resolveOrderProperty(BookOrderBy orderBy) {
    return switch (orderBy) {
      case TITLE -> "b.title";
      case PUBLISHED_DATE -> "b.publishedDate";
      case RATING -> "b.rating";
      case REVIEW_COUNT -> "b.reviewCount";
    };
  }

  private String resolveDirection(Sort.Direction direction) {
    return direction.isAscending() ? "ASC" : "DESC";
  }
}
