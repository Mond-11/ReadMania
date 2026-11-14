package ua.readmania;

import jakarta.servlet.http.Cookie;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

@TestConfiguration(proxyBeanMethods = false)
public class CsrfTestHelper {

    public static final String CSRF_TOKEN_VALUE = "test-csrf-token";

    public static RequestPostProcessor csrfToken() {
        return (MockHttpServletRequest request) -> {
            CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", CSRF_TOKEN_VALUE);
            request.setAttribute(CsrfToken.class.getName(), token);
            request.addHeader("X-XSRF-TOKEN", CSRF_TOKEN_VALUE);
            request.setCookies(new Cookie("XSRF-TOKEN", CSRF_TOKEN_VALUE));
            return request;
        };
    }

    @Bean
    public MockMvcConfigurer csrfMockMvcConfigurer() {
        return new MockMvcConfigurer() {
            @Override
            public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder, WebApplicationContext cxt) {
                return csrfToken();
            }
        };
    }
}
