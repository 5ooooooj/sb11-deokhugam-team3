package com.team3.deokhugam.global.lock.repository;

import com.team3.deokhugam.global.lock.domain.ApplicationLockEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationLockRepository
    extends JpaRepository<ApplicationLockEntity, UUID>, ApplicationLockRepositoryCustom {
}