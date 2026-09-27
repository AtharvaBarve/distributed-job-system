package com.atharva.com.distributedjobsystem.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobStatus;
import com.atharva.com.distributedjobsystem.repository.JobRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RedisQueueService {

    public static final String READY_QUEUE = "job_queue";
    public static final String PROCESSING_QUEUE = "job_queue:processing";

    private final StringRedisTemplate redisTemplate;
    private final JobRepository jobRepository;
    private final long recoveryTimeoutMs;
    private final boolean recoveryEnabled;

    public RedisQueueService(StringRedisTemplate redisTemplate, JobRepository jobRepository,
                             @Value("${jobs.recovery-timeout-ms:30000}") long recoveryTimeoutMs,
                             @Value("${jobs.recovery-enabled:true}") boolean recoveryEnabled) {
        this.redisTemplate = redisTemplate;
        this.jobRepository = jobRepository;
        this.recoveryTimeoutMs = recoveryTimeoutMs;
        this.recoveryEnabled = recoveryEnabled;
    }

    public void enqueue(String jobId) {
        redisTemplate.opsForList().rightPush(READY_QUEUE, jobId);
    }

    public String moveToProcessing() {
        return redisTemplate.opsForList().rightPopAndLeftPush(READY_QUEUE, PROCESSING_QUEUE);
    }

    public void acknowledge(String jobId) {
        redisTemplate.opsForList().remove(PROCESSING_QUEUE, 1, jobId);
    }

    /** Requeues jobs left in the processing list after a worker crash. */
    @Scheduled(fixedDelayString = "${jobs.recovery-interval-ms:5000}")
    public void recoverStaleJobs() {
        if (!recoveryEnabled) return;
        List<String> processingIds = redisTemplate.opsForList().range(PROCESSING_QUEUE, 0, -1);
        if (processingIds == null) {
            return;
        }

        Instant cutoff = Instant.now().minus(Duration.ofMillis(recoveryTimeoutMs));
        for (String id : processingIds) {
            try {
                Job job = jobRepository.findById(UUID.fromString(id)).orElse(null);
                if (job != null && job.getStatus() == JobStatus.PROCESSING
                        && job.getStartedAt() != null && job.getStartedAt().isBefore(cutoff)) {
                    job.setStatus(JobStatus.QUEUED);
                    jobRepository.save(job);
                    acknowledge(id);
                    enqueue(id);
                } else if (job == null || job.getStatus() == JobStatus.COMPLETED
                        || job.getStatus() == JobStatus.FAILED) {
                    acknowledge(id);
                } else if (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RETRYING) {
                    // Covers a crash between persisting a retry state and acknowledging the processing entry.
                    acknowledge(id);
                    job.setStatus(JobStatus.QUEUED);
                    jobRepository.save(job);
                    enqueue(id);
                }
            } catch (IllegalArgumentException ignored) {
                acknowledge(id);
            }
        }
    }
}
