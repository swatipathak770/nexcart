package com.nexcart.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiRateLimitFilterTest {
    @Test
    void limitsRegistrationAndLeavesSignedWebhookRouteUnthrottled() throws Exception {
        ApiRateLimitFilter filter = new ApiRateLimitFilter();
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = submit(filter, "/api/auth/register", new MockFilterChain());
            assertEquals(200, response.getStatus());
        }

        MockHttpServletResponse limited = submit(filter, "/api/auth/register", new MockFilterChain());
        assertEquals(429, limited.getStatus());
        assertEquals("60", limited.getHeader("Retry-After"));

        MockFilterChain webhookChain = new MockFilterChain();
        MockHttpServletResponse webhookResponse = submit(filter, "/api/webhooks/razorpay", webhookChain);
        assertEquals(200, webhookResponse.getStatus());
        assertNotNull(webhookChain.getRequest());
    }

    private MockHttpServletResponse submit(ApiRateLimitFilter filter, String path, FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}
