package com.eventdriven.order.redis;

import com.eventdriven.order.config.CartProperties;
import com.eventdriven.order.redis.model.GuestCart;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestCartRedisService {

    private static final String CART_KEY_PREFIX = "cart:guest:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CartProperties cartProperties;

    public Optional<GuestCart> getCart(String guestCartId) {
        String key = CART_KEY_PREFIX + guestCartId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, GuestCart.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize guest cart for key: {}", key, e);
            return Optional.empty();
        }
    }

    public void saveCart(GuestCart cart) {
        String key = CART_KEY_PREFIX + cart.getGuestCartId();
        try {
            String json = objectMapper.writeValueAsString(cart);
            long days = cartProperties.ttlDays() > 0 ? cartProperties.ttlDays() : 7;
            redisTemplate.opsForValue().set(key, json, Duration.ofDays(days));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize guest cart for key: {}", key, e);
            throw new RuntimeException("Could not persist guest cart in Redis", e);
        }
    }

    public void deleteCart(String guestCartId) {
        String key = CART_KEY_PREFIX + guestCartId;
        redisTemplate.delete(key);
    }
}
