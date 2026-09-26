package com.atharva.com.distributedjobsystem.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisQueueService {

    private static final String QUEUE_NAME = "job_queue";

    private final StringRedisTemplate redisTemplate;

    public RedisQueueService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    public void enqueue(String jobId) {
        redisTemplate.opsForList().rightPush(QUEUE_NAME, jobId);
    }
}