package com.eventdriven.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class RateLimiterService {

    @Value("${rate-limiter.enabled:true}")
    private boolean enabled;

    @Value("${rate-limiter.limit-per-minute:120}")
    private int limitPerMinute;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ConcurrentHashMap<String, AtomicInteger> inMemoryCounts = new ConcurrentHashMap<>();

    public boolean isAllowed(String clientIdentifier) {
        if (!enabled) {
            return true;
        }

        String key = "rate:limiter:" + clientIdentifier;

        if (redisTemplate != null) {
            try {
                Long currentCount = redisTemplate.opsForValue().increment(key);
                if (currentCount != null && currentCount == 1L) {
                    redisTemplate.expire(key, Duration.ofMinutes(1));
                }
                return currentCount != null && currentCount <= limitPerMinute;
            } catch (Exception e) {
                log.warn("Redis rate limiter unavailable: {}. Falling back to in-memory check.", e.getMessage());
            }
        }

        AtomicInteger counter = inMemoryCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= limitPerMinute;
    }
}
