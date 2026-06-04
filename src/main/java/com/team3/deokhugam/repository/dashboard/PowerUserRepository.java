package com.team3.deokhugam.repository.dashboard;

import com.team3.deokhugam.domain.dashboard.PowerUser;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PowerUserRepository extends JpaRepository<PowerUser, UUID> {

}
