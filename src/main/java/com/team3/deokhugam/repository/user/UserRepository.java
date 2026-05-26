package com.team3.deokhugam.repository.user;

import com.team3.deokhugam.domain.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
