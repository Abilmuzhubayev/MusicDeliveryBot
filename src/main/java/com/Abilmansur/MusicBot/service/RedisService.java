package com.Abilmansur.MusicBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    public void setValue(final String key, String value) {
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
    }

    public String getValue(final String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteKeyFromRedis(String key) {
        redisTemplate.delete(key);
    }
}
