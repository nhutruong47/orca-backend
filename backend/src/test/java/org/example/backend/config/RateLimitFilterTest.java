package org.example.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho RateLimitFilter — kiểm tra Bucket4j logic và HTTP response.
 *
 * <p>Test với mock FilterChain để verify chain.doFilter có được gọi không, status
 * code trả về, headers X-RateLimit-* có đặt đúng không.
 */
class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
        chain = mock(FilterChain.class);
    }

    private void invoke(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        filter.doFilter(req, res, chain);
    }

    @Test
    @DisplayName("GET /api/something thông thường → pass through chain")
    void normalRequest_passes() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();

        invoke(req, res);

        verify(chain).doFilter(any(), any());
        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getHeader("X-RateLimit-Remaining")).isNotNull();
    }

    @Test
    @DisplayName("POST /api/auth/login gọi 11 lần → lần 11 trả 429")
    void loginRateLimit() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr("5.6.7.8");
        MockHttpServletResponse res = new MockHttpServletResponse();

        // First 10 requests pass
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse r = new MockHttpServletResponse();
            invoke(req, r);
        }
        // 11th must be blocked
        invoke(req, res);

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("X-RateLimit-Retry-After-Seconds")).isNotNull();
        assertThat(res.getContentAsString()).contains("Quá nhiều yêu cầu");
        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Khác IP thì bucket riêng — IP1 bị block, IP2 vẫn pass")
    void differentIps_haveSeparateBuckets() throws Exception {
        // IP1 attack 10 times
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/auth/login");
            r.setRemoteAddr("10.0.0.1");
            invoke(r, new MockHttpServletResponse());
        }
        // IP2 still has budget
        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/auth/login");
        req2.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        invoke(req2, res2);
        assertThat(res2.getStatus()).isEqualTo(200);

        // IP1 11th is blocked
        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/auth/login");
        req1.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        invoke(req1, res1);
        assertThat(res1.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("/uploads/ là excluded path — không áp dụng rate limit")
    void uploadsAreExcluded() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/uploads/abc.jpg");
        req.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        invoke(req, res);
        verify(chain).doFilter(any(), any());
        assertThat(res.getStatus()).isEqualTo(200);
        // No rate limit headers because chain was unfiltered
        assertThat(res.getHeader("X-RateLimit-Remaining")).isNull();
    }

    @Test
    @DisplayName("WebSocket /ws path là excluded")
    void websocketIsExcluded() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/ws/notification");
        req.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        invoke(req, res);
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("X-Forwarded-For header được dùng khi có")
    void xForwardedForUsed() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("X-Forwarded-For", "203.0.113.50, 10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        invoke(req, res);
        // IP 203.0.113.50 from XFF gets its first bucket
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Bucket default: 600/ph — 600 pass, 601 fail")
    void defaultBucketLimit() throws Exception {
        // Drain 600
        for (int i = 0; i < 600; i++) {
            MockHttpServletRequest r = new MockHttpServletRequest("GET", "/api/orders");
            r.setRemoteAddr("8.8.8.8");
            invoke(r, new MockHttpServletResponse());
        }
        // 601st
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
        req.setRemoteAddr("8.8.8.8");
        MockHttpServletResponse res = new MockHttpServletResponse();
        invoke(req, res);
        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("Login bucket riêng biệt với default — login xong vẫn gọi được API khác")
    void loginBucketDoesNotAffectDefault() throws Exception {
        // 10 login attempts
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/auth/login");
            r.setRemoteAddr("9.9.9.9");
            invoke(r, new MockHttpServletResponse());
        }
        // Default bucket still has 600
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
        req.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse res = new MockHttpServletResponse();
        invoke(req, res);
        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getHeader("X-RateLimit-Remaining")).isEqualTo("599");
    }
}
