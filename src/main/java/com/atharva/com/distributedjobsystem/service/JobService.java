package com.atharva.com.distributedjobsystem.service;

import com.atharva.com.distributedjobsystem.dto.CreateJobRequest;
import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobStatus;
import com.atharva.com.distributedjobsystem.repository.JobRepository;
import com.atharva.com.distributedjobsystem.exception.JobNotFoundException;
import com.atharva.com.distributedjobsystem.observability.JobMetrics;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final RedisQueueService redisQueueService;
    private final int maxAttempts;
    private final JobMetrics metrics;


    public JobService(JobRepository jobRepository, RedisQueueService redisQueueService,
                      @Value("${jobs.max-attempts:3}") int maxAttempts,
                      JobMetrics metrics) {
        this.redisQueueService = redisQueueService;
        this.jobRepository = jobRepository;
        this.maxAttempts = maxAttempts;
        this.metrics = metrics;
    }

    public Job createJob(CreateJobRequest request) {

        Job job = new Job();

        job.setType(request.type());
        job.setPayload(request.payload());
        job.setStatus(JobStatus.QUEUED);

        job.setAttempts(0);
        job.setMaxAttempts(maxAttempts);

        job.setCreatedAt(Instant.now());

        Job savedJob = jobRepository.save(job);

        redisQueueService.enqueue(savedJob.getId().toString());
        metrics.submitted();

        return savedJob;
    }

    public Job getJob(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }
}
