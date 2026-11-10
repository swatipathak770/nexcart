package com.nexcart.security;

import com.nexcart.config.JwtProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-that-is-at-least-thirty-two-characters-long";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicAuthenticationAndWebhookRoutesIgnoreBearerTokens() throws Exception {
        JwtAuthenticationFilter filter = filter(jwtService());

        for (String path : new String[]{"/api/auth/register", "/api/auth/login", "/api/webhooks/razorpay"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
            request.setServletPath(path);
            request.addHeader("Authorization", "Bearer stale-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertEquals(200, response.getStatus(), path);
            assertNotNull(chain.getRequest(), path);
        }
    }

    @Test
    void invalidBearerTokenIsRejectedForProtectedRoutes() throws Exception {
        JwtAuthenticationFilter filter = filter(jwtService());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.setServletPath("/api/user/profile");
        request.addHeader("Authorization", "Bearer malformed-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void validBearerTokenAuthenticatesProtectedRequests() throws Exception {
        JwtService jwtService = jwtService();
        UserDetails principal = User.withUsername("customer@example.com")
                .password("unused")
                .authorities("ROLE_CUSTOMER")
                .build();
        String token = jwtService.generateToken(principal);
        JwtAuthenticationFilter filter = filter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.setServletPath("/api/user/profile");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        assertEquals("customer@example.com", SecurityContextHolder.getContext()
                .getAuthentication().getName());
    }

    private JwtAuthenticationFilter filter(JwtService jwtService) {
        CustomUserDetailsService users = new CustomUserDetailsService(null) {
            @Override
            public UserDetails loadUserByUsername(String email) {
                return User.withUsername(email)
                        .password("unused")
                        .authorities("ROLE_CUSTOMER")
                        .build();
            }
        };
        return new JwtAuthenticationFilter(jwtService, users);
    }

    private JwtService jwtService() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpiration(60_000);
        return new JwtService(properties);
    }
}
