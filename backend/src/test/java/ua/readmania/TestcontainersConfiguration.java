package ua.readmania;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
                    .withDatabaseName("readmania_test")
                    .withUsername("readmania")
                    .withPassword("readmania");

    static {
        POSTGRES_CONTAINER.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES_CONTAINER;
    }

    @Bean
    org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer csrfMockMvcBuilderCustomizer() {
        return builder -> builder.apply(new MockMvcConfigurer() {
            @Override
            public org.springframework.test.web.servlet.request.RequestPostProcessor beforeMockMvcCreated(
                    ConfigurableMockMvcBuilder<?> builder, WebApplicationContext cxt) {
                return CsrfTestHelper.csrfToken();
            }
        });
    }
}
