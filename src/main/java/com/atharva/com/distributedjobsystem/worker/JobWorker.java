package com.atharva.com.distributedjobsystem.worker;

import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobStatus;
import com.atharva.com.distributedjobsystem.entity.JobType;
import com.atharva.com.distributedjobsystem.repository.JobRepository;
import com.atharva.com.distributedjobsystem.service.RedisQueueService;
import com.atharva.com.distributedjobsystem.observability.JobMetrics;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class JobWorker {
    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);
    private final JobRepository jobRepository;
    private final RedisQueueService redisQueueService;
    private final List<JobHandler> jobHandlers;
    private final ThreadPoolTaskExecutor jobTaskExecutor;
    private final int workerCount;
    private final long retryBaseDelayMs;
    private final JobMetrics metrics;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public JobWorker(JobRepository jobRepository, RedisQueueService redisQueueService,
                     List<JobHandler> jobHandlers, ThreadPoolTaskExecutor jobTaskExecutor,
                     @Value("${jobs.worker-count:4}") int workerCount,
                     @Value("${jobs.retry-base-delay-ms:1000}") long retryBaseDelayMs,
                     JobMetrics metrics) {
        this.jobRepository = jobRepository;
        this.redisQueueService = redisQueueService;
        this.jobHandlers = jobHandlers;
        this.jobTaskExecutor = jobTaskExecutor;
        this.workerCount = workerCount;
        this.retryBaseDelayMs = retryBaseDelayMs;
        this.metrics = metrics;
    }

    private JobHandler getHandler(JobType type) {
        return jobHandlers.stream().filter(handler -> handler.getType() == type).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No handler found for job type: " + type));
    }

    public void processJobs() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                if (!processNextJob()) Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception exception) {
                log.error("Worker loop failed; continuing", exception);
            }
        }
    }
    public boolean processNextJob() {
        String jobId = redisQueueService.moveToProcessing();
        if (jobId == null) return false;

        UUID id;
        try {
            id = UUID.fromString(jobId);
        } catch (IllegalArgumentException exception) {
            log.warn("Discarding malformed job ID from Redis: {}", jobId);
            redisQueueService.acknowledge(jobId);
            return true;
        }

        // Conditional claiming makes duplicate delivery safe: only one delivery can claim a QUEUED job.
        if (jobRepository.claimForProcessing(id) == 0) {
            log.info("Skipping duplicate or already terminal job {}", id);
            redisQueueService.acknowledge(jobId);
            return true;
        }

        Job job = jobRepository.findById(id).orElse(null);
        if (job == null) {
            redisQueueService.acknowledge(jobId);
            return true;
        }

        job.setStartedAt(Instant.now());
        job = jobRepository.save(job);

        log.info("Worker {} processing job {} attempt={} type={}",
                Thread.currentThread().getName(), job.getId(), job.getAttempts(), job.getType());
        metrics.started();
        Timer.Sample timer = metrics.startTimer();
        try {
            if (shouldFail(job)) throw new IllegalStateException("Simulated job failure");
            getHandler(job.getType()).execute(job);
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job = jobRepository.save(job);
            redisQueueService.acknowledge(jobId);
            metrics.completed();
            log.info("Job {} completed", job.getId());
        } catch (Exception exception) {
            handleFailure(job, jobId, exception);
        } finally {
            metrics.stopped();
            metrics.record(timer);
        }
        return true;
    }

    private void handleFailure(Job job, String jobId, Exception exception) {
        boolean retry = job.getAttempts() < job.getMaxAttempts();
        job.setErrorMessage(exception.getMessage());

        if (!retry) {
            job.setStatus(JobStatus.FAILED);
            job = jobRepository.save(job);

            redisQueueService.acknowledge(jobId);
            metrics.failed();

            log.error(
                    "Job {} reached max attempts={} and is permanently failed",
                    job.getId(),
                    job.getMaxAttempts()
            );
            return;
        }

        job.setStatus(JobStatus.RETRYING);
        metrics.retried();

        job = jobRepository.save(job);
        redisQueueService.acknowledge(jobId);

        long delay = retryBaseDelayMs *
                (1L << Math.min(job.getAttempts() - 1, 10));

        log.warn(
                "Job {} failed attempt={}, retrying after {} ms",
                job.getId(),
                job.getAttempts(),
                delay
        );

        try {
            Thread.sleep(delay);

            job.setStatus(JobStatus.QUEUED);
            job = jobRepository.save(job);

            redisQueueService.enqueue(jobId);

        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.warn("Retry interrupted for job {}", job.getId());
        }
    }

    private boolean shouldFail(Job job) {
        return job.getPayload() != null && job.getPayload().contains("\"fail\": true");
    }

    @PostConstruct
    public void startWorkers() {
        for (int i = 0; i < workerCount; i++) jobTaskExecutor.execute(this::processJobs);
        log.info("Started {} job workers", workerCount);
    }

    @PreDestroy
    public void stopWorkers() {
        running.set(false);
        jobTaskExecutor.shutdown();
        log.info("Stopped job workers");
    }
}
