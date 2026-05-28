package com.team3.deokhugam.repository.user;

import com.team3.deokhugam.domain.user.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByEmail(String email);

  @Query("""
    select u
    from User u
    where u.email = :email
      and u.deletedAt is null
    """)
  Optional<User> findActiveByEmail(@Param("email") String email);

  @Query("""
      select u
      from User u
      where u.id = :id
      and u.deletedAt is null
      """)
  Optional<User> findActiveById(@Param("id") UUID id);

  @Modifying
  @Query("""
  delete from User u
  where u.deletedAt is not null
    and u.deletedAt <= :deleteBefore
  """)
  int deleteExpiredSoftDeletedUsers(@Param("deleteBefore") Instant deleteBefore);
}
