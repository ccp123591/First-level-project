package com.fitcoach.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    @Bean(name = "bytesRedisTemplate")
    public RedisTemplate<String, byte[]> bytesRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, byte[]> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);
        tpl.setKeySerializer(RedisSerializer.string());
        tpl.setHashKeySerializer(RedisSerializer.string());
        tpl.setValueSerializer(RedisSerializer.byteArray());
        tpl.setHashValueSerializer(RedisSerializer.byteArray());
        tpl.afterPropertiesSet();
        return tpl;
    }
}
