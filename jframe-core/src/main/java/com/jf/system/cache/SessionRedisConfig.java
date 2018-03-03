package com.jf.system.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Created with IntelliJ IDEA.
 * Description: Spring Redis Session
 * User: xujunfei
 * Date: 2018-01-03
 * Time: 10:42
 */
//@Configuration
//@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionRedisConfig {
}