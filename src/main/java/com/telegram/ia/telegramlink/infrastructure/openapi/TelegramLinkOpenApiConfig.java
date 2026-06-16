package com.telegram.ia.telegramlink.infrastructure.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramLinkOpenApiConfig {
    @Bean
    OpenAPI telegramIaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Telegram IA API")
                .version("v1")
                .description("Telegram linking MVP endpoints."));
    }
}
