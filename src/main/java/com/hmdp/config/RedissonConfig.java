package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        //1.创建配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://101.37.204.19:6379").setPassword("123321");
        //2.根据 Config 创建出 RedissonClient 实例
        return Redisson.create(config);
    }
}
