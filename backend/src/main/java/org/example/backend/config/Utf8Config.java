package org.example.backend.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.io.IOException;

/**
 * Ensures every request and response is encoded as UTF-8.
 *
 * Without this filter, Spring's default behaviour is to honour the request's
 * declared Content-Type charset, which can produce mojibake when the client
 * does not explicitly send a charset parameter (Tomcat falls back to
 * ISO-8859-1). This is the recommended fix for Vietnamese / CJK text
 * in Spring Boot 3.x REST APIs.
 */
@Configuration
public class Utf8Config {

    /**
     * Primary character-encoding filter. Forces UTF-8 on request bodies
     * before the controller / Jackson layer reads them.
     *
     * <p>The bean name is intentionally different from Spring Boot's
     * auto-configured {@code characterEncodingFilter} to avoid a
     * {@link org.springframework.beans.factory.support.BeanDefinitionOverrideException}
     * on application start. The bean name {@code orcaCharacterEncodingFilter}
     * is also registered with higher precedence than the autoconfigured one,
     * so it wins regardless of bean-discovery order.
     */
    @Bean("orcaCharacterEncodingFilter")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Filter characterEncodingFilter() {
        CharacterEncodingFilter f = new CharacterEncodingFilter();
        f.setEncoding("UTF-8");
        f.setForceEncoding(true);
        f.setForceRequestEncoding(true);
        f.setForceResponseEncoding(true);
        return f;
    }

    /**
     * Secondary filter: belt-and-braces override of response Content-Type
     * charset for any path that returns JSON. Some servlet stacks only
     * append ;charset=... when the controller's produces = ... declares it.
     */
    @Bean("orcaResponseCharsetOverrideFilter")
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public Filter responseCharsetOverrideFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                    throws IOException, ServletException {
                if (res instanceof HttpServletResponse http) {
                    String ct = http.getContentType();
                    if (ct != null && ct.toLowerCase().contains("application/json") && !ct.toLowerCase().contains("charset")) {
                        http.setContentType(ct + ";charset=UTF-8");
                    }
                }
                chain.doFilter(req, res);
            }
        };
    }
}