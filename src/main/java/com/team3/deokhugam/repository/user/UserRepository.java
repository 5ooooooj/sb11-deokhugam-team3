package com.team3.deokhugam.repository.user;

import com.team3.deokhugam.domain.user.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

  boolean existsByEmail(String email);

}
