package ua.readmania.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private DataSource dataSource;

    @Test
    @DisplayName("D3: PasswordEncoder is BCryptPasswordEncoder")
    void testPasswordEncoderIsBCrypt() {
        assertThat(passwordEncoder).isNotNull();
        String rawPassword = "securePassword123!";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoded).startsWith("$2a$");
        assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
    }

    @Test
    @DisplayName("Anonymous access to /api/orders returns 401 with Problem Details JSON")
    void anonymousAccessToOrdersReturns401WithProblemDetailsJson() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.instance", is("/api/orders")))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("USER role access to /api/manager/books returns 403 with Problem Details JSON")
    @WithMockUser(roles = "USER")
    void userRoleAccessToManagerBooksReturns403WithProblemDetailsJson() throws Exception {
        mockMvc.perform(get("/api/manager/books"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.instance", is("/api/manager/books")))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("MANAGER role access to /api/manager/books is permitted (not 401/403)")
    @WithMockUser(roles = "MANAGER")
    void managerRoleAccessToManagerBooksIsPermitted() throws Exception {
        mockMvc.perform(get("/api/manager/books"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("Anonymous access to /api/catalog/books is permitted (not 401)")
    void anonymousAccessToCatalogBooksIsPermitted() throws Exception {
        mockMvc.perform(get("/api/catalog/books"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("Actuator health endpoint /actuator/health is open and returns 200 without details")
    void actuatorHealthReturns200WithoutDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("Anonymous access to /api/auth/login and /api/auth/register is permitted (not 401)")
    void anonymousAccessToAuthEndpointsIsPermitted() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));

        mockMvc.perform(post("/api/auth/register"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("Swagger endpoints are closed in non-local profiles by default (anonymous 401, authenticated 403)")
    void swaggerClosedByDefault() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Swagger endpoints return 403 for authenticated users when closed")
    @WithMockUser
    void swaggerForbiddenForAuthenticatedUsersByDefault() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.title", is("Forbidden")));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.title", is("Forbidden")));
    }

    @Test
    @DisplayName("Default deny for unlisted /api/** endpoints (e.g. /api/account)")
    void defaultDenyForUnlistedApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.title", is("Unauthorized")));
    }
}
