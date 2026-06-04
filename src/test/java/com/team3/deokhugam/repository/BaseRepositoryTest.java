package com.team3.deokhugam.repository;

import com.team3.deokhugam.global.config.JpaAuditingConfig;
import com.team3.deokhugam.repository.review.ReviewRepositoryCustomImpl;
import com.team3.deokhugam.repository.user.UserRepositoryCustomImpl;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    JpaAuditingConfig.class,
    ReviewRepositoryCustomImpl.class,
    UserRepositoryCustomImpl.class
})
public abstract class BaseRepositoryTest {
}