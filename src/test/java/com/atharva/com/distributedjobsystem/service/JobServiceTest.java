package com.atharva.com.distributedjobsystem.service;

import com.atharva.com.distributedjobsystem.dto.CreateJobRequest;
import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobStatus;
import com.atharva.com.distributedjobsystem.entity.JobType;
import com.atharva.com.distributedjobsystem.observability.JobMetrics;
import com.atharva.com.distributedjobsystem.repository.JobRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobServiceTest {
    private final JobRepository repository = mock(JobRepository.class);
    private final RedisQueueService queue = mock(RedisQueueService.class);
    private final JobMetrics metrics = mock(JobMetrics.class);

    @Test
    void createsAndEnqueuesQueuedJob() {
        UUID id = UUID.randomUUID();
        when(repository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            job.setId(id);
            return job;
        });

        Job result = new JobService(repository, queue, 3, metrics)
                .createJob(new CreateJobRequest(JobType.EMAIL, "{}"));

        assertEquals(id, result.getId());
        assertEquals(JobStatus.QUEUED, result.getStatus());
        assertEquals(3, result.getMaxAttempts());
        verify(queue).enqueue(id.toString());
        verify(metrics).submitted();
    }
}
