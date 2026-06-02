package com.team3.deokhugam.repository.user;

import static com.team3.deokhugam.domain.user.QUser.user;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team3.deokhugam.domain.user.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryCustomImpl implements UserRepositoryCustom{

  private final JPAQueryFactory queryFactory;

  public UserRepositoryCustomImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  // 회원가입
  @Override
  public Optional<User> findActiveByEmail(String email){
    User result = queryFactory
        .selectFrom(user)
        .where(
            user.email.eq(email),
            user.deletedAt.isNull()
        )
        .fetchOne();

    return Optional.ofNullable(result);

  }

  // 로그인
  @Override
  public Optional<User> findActiveById(UUID id){
    User result = queryFactory
        .selectFrom(user)
        .where(
            user.id.eq(id),
            user.deletedAt.isNull()
        )
        .fetchOne();

    return Optional.ofNullable(result);
  }

  // 논리삭제
  @Override
  public int deleteExpiredSoftDeletedUsers(Instant deleteBefore){
    long deletedCount = queryFactory
        .delete(user)
        .where(
            user.deletedAt.isNotNull(),
            user.deletedAt.loe(deleteBefore)
        )
        .execute();

    return Math.toIntExact(deletedCount);
  }

}
