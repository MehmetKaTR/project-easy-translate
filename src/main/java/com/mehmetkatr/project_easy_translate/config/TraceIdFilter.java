package com.mehmetkatr.project_easy_translate.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Her HTTP isteğine bir correlation/trace ID atar ve MDC'ye koyar.
 * Böylece o isteğe ait TÜM log satırları aynı traceId ile işaretlenir
 * (logback pattern'inde %X{traceId} ile basılır) ve yanıt header'ında da döner.
 * İstemci bir hata bildirince, aynı traceId ile sunucudaki tüm akış bulunabilir.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    private static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // İstemci kendi trace id'sini gönderdiyse onu kullan (dağıtık izleme), yoksa üret.
        String incoming = request.getHeader(HEADER);
        String traceId = (incoming == null || incoming.isBlank())
                ? UUID.randomUUID().toString()
                : incoming;

        MDC.put(TRACE_ID, traceId);
        response.setHeader(HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID); // thread havuzunda sızmayı önlemek için MUTLAKA temizle
        }
    }
}
