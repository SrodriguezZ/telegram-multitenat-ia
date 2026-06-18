package com.telegram.ia.telegramlink.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

class ApplicationYamlConfigurationTest {

    @Test
    void applicationYamlEnablesDevelopmentJpaUpdateAndTelegramLinkProperties() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(
                new FileSystemResource("src/main/resources/application.yaml"),
                new FileSystemResource("src/main/resources/application-dev.yaml"));
        Properties properties = yaml.getObject();

        assertThat(properties)
                .containsEntry("spring.profiles.active", "dev")
                .containsEntry("spring.datasource.url", "jdbc:postgresql://localhost:5432/telegram_ia")
                .containsEntry("spring.datasource.username", "root")
                .containsEntry("spring.datasource.password", "admin")
                .containsEntry("spring.jpa.hibernate.ddl-auto", "update")
                .containsEntry("telegram-link.invitation.ttl", "48h")
                .containsKey("telegram-link.bot.username")
                .containsKey("telegram-link.token.pepper")
                .containsKey("telegram-link.current-user.company-user-id")
                .containsKey("telegram-link.current-user.company-id")
                .containsKey("telegram-link.current-user.role")
                .containsKey("telegram-link.current-user.status");
    }

    @Test
    void springdocOpenApiClassesAreAvailable() throws Exception {
        assertThat(Class.forName("io.swagger.v3.oas.models.OpenAPI")).isNotNull();
    }

    @Test
    void productionYamlRequiresEnvironmentBackedSecretsAndValidatesSchema() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application-prod.yaml"));
        Properties properties = yaml.getObject();

        assertThat(properties)
                .containsEntry("spring.datasource.url", "${TELEGRAM_IA_DATASOURCE_URL}")
                .containsEntry("spring.datasource.username", "${TELEGRAM_IA_DATASOURCE_USERNAME}")
                .containsEntry("spring.datasource.password", "${TELEGRAM_IA_DATASOURCE_PASSWORD}")
                .containsEntry("spring.jpa.hibernate.ddl-auto", "validate")
                .containsEntry("telegram-link.token.pepper", "${TELEGRAM_LINK_TOKEN_PEPPER}");
    }
}
