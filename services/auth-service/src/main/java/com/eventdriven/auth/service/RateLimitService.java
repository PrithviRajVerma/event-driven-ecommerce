package com.eventdriven.auth.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;

    public  RateLimitService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean isAllowed(String key, int maxRequests, Duration window){
        Long count = stringRedisTemplate.opsForValue().increment(key);

        if(count != null && count ==1){
            stringRedisTemplate.expire(key,window);
        }

        return count!=null && count <= maxRequests;
    }


}
