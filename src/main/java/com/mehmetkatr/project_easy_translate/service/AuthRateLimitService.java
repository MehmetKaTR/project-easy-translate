package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.exception.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthRateLimitService {

    private static final class Bucket {
        long windowStartMs;
        int count;
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void assertAllowed(String key, int maxRequests, Duration window) {
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be positive");
        }
        long now = System.currentTimeMillis();
        long windowMs = Math.max(1L, window.toMillis());

        Bucket bucket = buckets.computeIfAbsent(key, ignored -> {
            Bucket created = new Bucket();
            created.windowStartMs = now;
            created.count = 0;
            return created;
        });

        long retryAfterSeconds = 0;
        synchronized (bucket) {
            if (now - bucket.windowStartMs >= windowMs) {
                bucket.windowStartMs = now;
                bucket.count = 0;
            }

            if (bucket.count >= maxRequests) {
                long remainingMs = Math.max(0L, windowMs - (now - bucket.windowStartMs));
                retryAfterSeconds = Math.max(1L, (long) Math.ceil(remainingMs / 1000.0));
            } else {
                bucket.count += 1;
            }
        }

        if (retryAfterSeconds > 0) {
            throw new RateLimitExceededException("Too many requests. Please try again later.", retryAfterSeconds);
        }

        if (buckets.size() > 20_000) {
            buckets.entrySet().removeIf(entry -> now - entry.getValue().windowStartMs > windowMs * 3);
        }
    }
}
