package com.mehmetkatr.project_easy_translate.security;

import com.mehmetkatr.project_easy_translate.entity.Admin;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.repository.AdminRepository;
import com.mehmetkatr.project_easy_translate.repository.UserRepository;
import com.mehmetkatr.project_easy_translate.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> EMAIL_VERIFICATION_EXEMPT_PATHS = List.of(
            "/api/auth/verify-email",
            "/api/auth/resend-verification",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/social/google",
            "/api/auth/social/google/dev",
            "/api/admin/auth/login",
            "/api/admin/auth/status",
            "/api/admin/auth/bootstrap",
            "/api/public/"
    );

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String bearerToken = request.getHeader("Authorization");

        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = bearerToken.substring(7);
        if (!tokenService.validateToken(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        String subject = tokenService.getUsernameFromToken(jwt);
        String role = tokenService.getRoleFromToken(jwt);
        String normalizedRole = role != null && role.startsWith("ROLE_") ? role : "ROLE_USER";

        if ("ROLE_USER".equals(normalizedRole)) {
            Optional<User> userOpt = userRepository.findFirstByUsernameIgnoreCase(subject);
            if (userOpt.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            User user = userOpt.get();
            if (!user.isEmailVerified() && !isVerificationExempt(request.getRequestURI())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"EMAIL_NOT_VERIFIED\",\"error\":\"Email is not verified. Please verify your email first.\"}");
                return;
            }
        }

        if ("ROLE_ADMIN".equals(normalizedRole)) {
            Optional<Admin> adminOpt = adminRepository.findFirstByUsernameIgnoreCase(subject);
            if (adminOpt.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        subject,
                        null,
                        List.of(new SimpleGrantedAuthority(normalizedRole))
                );

        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }

    private boolean isVerificationExempt(String uri) {
        if (uri == null) return false;
        for (String path : EMAIL_VERIFICATION_EXEMPT_PATHS) {
            if (uri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }
}
