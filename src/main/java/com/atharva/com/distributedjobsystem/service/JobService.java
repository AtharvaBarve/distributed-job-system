package com.atharva.com.distributedjobsystem.service;

import com.atharva.com.distributedjobsystem.dto.CreateJobRequest;
import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobStatus;
import com.atharva.com.distributedjobsystem.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final RedisQueueService redisQueueService;


    public JobService(JobRepository jobRepository , RedisQueueService redisQueueService) {
        this.redisQueueService = redisQueueService;
        this.jobRepository = jobRepository;
    }

    public Job createJob(CreateJobRequest request) {

        Job job = new Job();

        job.setType(request.type());
        job.setPayload(request.payload());
        job.setStatus(JobStatus.QUEUED);

        job.setAttempts(0);
        job.setMaxAttempts(3);

        job.setCreatedAt(Instant.now());

        Job savedJob = jobRepository.save(job);

        redisQueueService.enqueue(savedJob.getId().toString());

        return savedJob;
    }

    public Job getJob(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow();
    }
}