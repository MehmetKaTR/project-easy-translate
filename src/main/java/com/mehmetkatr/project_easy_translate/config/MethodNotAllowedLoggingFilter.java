package com.mehmetkatr.project_easy_translate.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class MethodNotAllowedLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MethodNotAllowedLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (response.getStatus() == HttpServletResponse.SC_METHOD_NOT_ALLOWED) {
                String query = request.getQueryString();
                String uri = request.getRequestURI() + (query != null ? "?" + query : "");
                log.warn("405 Method Not Allowed: {} {} from {}", request.getMethod(), uri, request.getRemoteAddr());
            }
        }
    }
}
