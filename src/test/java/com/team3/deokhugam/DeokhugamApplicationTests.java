package com.team3.deokhugam;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;

@EnableRetry
@SpringBootTest
@ActiveProfiles("test")
class DeokhugamApplicationTests {

	@Test
	void contextLoads() {
	}

}
