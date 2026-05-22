package com.team3.deokhugam.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI openAPI() {
    // Deokhugam-Request-User-ID 헤더를 전역 파라미터로 등록
    SecurityScheme securityScheme = new SecurityScheme()
        .type(Type.APIKEY)
        .in(In.HEADER)
        .name("Deokhugam-Request-User-ID");

    return new OpenAPI()
        .info(new Info()
            .title("덕후감 API")
            .description("도서 리뷰 커뮤니티 서비스 API")
            .version("v1"))
        .addSecurityItem(new SecurityRequirement()
            .addList("Deokhugam-Request-User-ID"))
        .components(new Components()
                .addSecuritySchemes("Deokhugam-Request-User-ID", securityScheme));
  }

}
