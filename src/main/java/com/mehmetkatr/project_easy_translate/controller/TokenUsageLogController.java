package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.response.TokenUsageReportResponse;
import com.mehmetkatr.project_easy_translate.service.TokenUsageLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Admin: kullanıcı bazlı token kullanım raporu.
 * ADMIN rolü ile korunur (/api/admin/** SecurityConfig'te kısıtlı).
 */
@RestController
@RequestMapping("/api/admin/token-usage")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TokenUsageLogController {

    private final TokenUsageLogService tokenUsageLogService;

    @GetMapping("/{userId}")
    public ResponseEntity<TokenUsageReportResponse> getUsage(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int days
    ) {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusDays(days);
        int tokensUsed = tokenUsageLogService.sumTokensByUserIdBetween(userId, from, to);
        long generationCount = tokenUsageLogService.countByUserIdBetween(userId, from, to);
        return ResponseEntity.ok(new TokenUsageReportResponse(userId, days, tokensUsed, generationCount, from, to));
    }
}
