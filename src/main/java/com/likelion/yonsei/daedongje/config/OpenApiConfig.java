package com.likelion.yonsei.daedongje.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI daedongjeOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("무악대동제 2026 API")
						.description("연세대학교 개교 141주년 무악대동제 2026 백엔드 API 문서")
						.version("v0.0.1"));
	}
}
