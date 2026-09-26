package com.atharva.com.distributedjobsystem.worker;

import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobStatus;
import com.atharva.com.distributedjobsystem.entity.JobType;
import com.atharva.com.distributedjobsystem.repository.JobRepository;
import com.atharva.com.distributedjobsystem.service.RedisQueueService;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class JobWorker {

    private static final String QUEUE_NAME = "job_queue";

    private final StringRedisTemplate redisTemplate;

    private final JobRepository jobRepository;

    private final RedisQueueService redisQueueService;

    private final List<JobHandler> jobHandlers;

    private final ThreadPoolTaskExecutor jobTaskExecutor;


    public JobWorker(StringRedisTemplate redisTemplate , JobRepository jobRepository , RedisQueueService redisQueueService , List<JobHandler> jobHandlers , ThreadPoolTaskExecutor jobTaskExecutor) {
        this.redisTemplate = redisTemplate;
        this.jobRepository = jobRepository;
        this.redisQueueService = redisQueueService;
        this.jobHandlers = jobHandlers;
        this.jobTaskExecutor = jobTaskExecutor;
    }

    private JobHandler getHandler(JobType type) {
        return jobHandlers.stream()
                .filter(handler -> handler.getType() == type)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No handler found for job type: " + type
                        )
                );
    }

    public void processJobs() {
        while (!Thread.currentThread().isInterrupted()) {

            String jobId = redisTemplate.opsForList().leftPop(QUEUE_NAME);

            if (jobId == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            Job job = jobRepository.findById(UUID.fromString(jobId))
                    .orElse(null);

            if (job != null) {
                job.setStatus(JobStatus.PROCESSING);
                job.setAttempts(job.getAttempts() + 1);
                job.setStartedAt(Instant.now());

                jobRepository.save(job);

                System.out.println(
                        Thread.currentThread().getName()
                                + " processing job: " + job.getId()
                );

                if (shouldFail(job)) {
                    job.setStatus(JobStatus.FAILED);
                    job.setErrorMessage("Simulated job failure");

                    jobRepository.save(job);

                    System.out.println("Job failed: " + job.getId());

                    if (job.getAttempts() < job.getMaxAttempts()) {
                        redisQueueService.enqueue(job.getId().toString());

                        System.out.println("Retrying job: " + job.getId());
                    }

                    continue;
                }
                try {
                    JobHandler handler = getHandler(job.getType());

                    handler.execute(job);

                    job.setStatus(JobStatus.COMPLETED);
                    job.setCompletedAt(Instant.now());

                    jobRepository.save(job);

                    System.out.println(
                            Thread.currentThread().getName()
                                    + " completed job: " + job.getId()
                    );
                } catch (Exception e) {
                    job.setStatus(JobStatus.FAILED);
                    job.setErrorMessage(e.getMessage());

                    jobRepository.save(job);

                    System.out.println("Job failed: " + job.getId());

                    if (job.getAttempts() < job.getMaxAttempts()) {
                        redisQueueService.enqueue(job.getId().toString());
                        System.out.println("Retrying job: " + job.getId());
                    }
                }
            }
        }
    }
    private boolean shouldFail(Job job) {
        return job.getPayload() != null &&
                job.getPayload().contains("\"fail\": true");
    }
    @PostConstruct
    public void startWorkers() {
        for (int i = 0; i < 4; i++) {
            jobTaskExecutor.execute(this::processJobs);
        }
    }
}