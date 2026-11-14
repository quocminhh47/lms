package com.lms.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class ProductionConfigurationTest {

    @Test
    void productionRequiresExplicitDatabaseAndCorsTargets() throws IOException {
        MutablePropertySources sources = new MutablePropertySources();
        sources.addLast(new PropertiesPropertySource("prod",
                PropertiesLoaderUtils.loadAllProperties("application-prod.properties")));
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
        for (String key : new String[] {
                "spring.datasource.url", "spring.datasource.username", "app.cors.allowed-origins" }) {
            assertThatThrownBy(() -> resolver.getRequiredProperty(key))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        sources.addFirst(new MapPropertySource("deployment", Map.of(
                "DB_URL", "jdbc:postgresql://database.example/test?sslmode=require",
                "DB_USERNAME", "test_user",
                "CORS_ALLOWED_ORIGINS", "https://ui.example.com",
                "PORT", "10000")));
        assertThat(resolver.getRequiredProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://database.example/test?sslmode=require");
        assertThat(resolver.getRequiredProperty("spring.datasource.username")).isEqualTo("test_user");
        assertThat(resolver.getRequiredProperty("app.cors.allowed-origins")).isEqualTo("https://ui.example.com");
        assertThat(resolver.getRequiredProperty("server.port")).isEqualTo("10000");
    }
}
