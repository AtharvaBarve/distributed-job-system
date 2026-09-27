package com.atharva.com.distributedjobsystem.worker;

import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobStatus;
import com.atharva.com.distributedjobsystem.entity.JobType;
import com.atharva.com.distributedjobsystem.observability.JobMetrics;
import com.atharva.com.distributedjobsystem.repository.JobRepository;
import com.atharva.com.distributedjobsystem.service.RedisQueueService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobWorkerTest {
    private final JobRepository repository = mock(JobRepository.class);
    private final RedisQueueService queue = mock(RedisQueueService.class);
    private final ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
    private final JobMetrics metrics = mock(JobMetrics.class);
    private final JobHandler handler = mock(JobHandler.class);
    private final UUID id = UUID.randomUUID();

    private JobWorker worker(Job job) {
        when(queue.moveToProcessing()).thenReturn(id.toString());
        when(repository.claimForProcessing(id)).thenReturn(1);
        when(repository.findById(id)).thenReturn(Optional.of(job));
        when(handler.getType()).thenReturn(JobType.EMAIL);
        return new JobWorker(repository, queue, java.util.List.of(handler), executor, 0, 0, metrics);
    }

    @Test
    void successfulJobBecomesCompleted() {
        Job job = job(1, 3);
        worker(job).processNextJob();
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        verify(handler).execute(job);
        verify(queue).acknowledge(id.toString());
    }

    @Test
    void failedJobIsRetriedWithQueuedState() {
        Job job = job(1, 3);
        doThrow(new IllegalStateException("dependency unavailable")).when(handler).execute(job);
        worker(job).processNextJob();
        assertEquals(JobStatus.QUEUED, job.getStatus());
        verify(queue).enqueue(id.toString());
        verify(metrics).retried();
    }

    @Test
    void maxAttemptsProducesFinalFailure() {
        Job job = job(3, 3);
        doThrow(new IllegalStateException("permanent failure")).when(handler).execute(job);
        worker(job).processNextJob();
        assertEquals(JobStatus.FAILED, job.getStatus());
        verify(queue, never()).enqueue(any());
    }

    @Test
    void duplicateDeliveryIsSkippedWhenClaimFails() {
        Job job = job(1, 3);
        worker(job);
        when(repository.claimForProcessing(id)).thenReturn(0);
        new JobWorker(repository, queue, java.util.List.of(handler), executor, 0, 0, metrics).processNextJob();
        verify(handler, never()).execute(job);
        verify(queue).acknowledge(id.toString());
    }

    private Job job(int attempts, int maxAttempts) {
        Job job = new Job();
        job.setId(id);
        job.setType(JobType.EMAIL);
        job.setStatus(JobStatus.QUEUED);
        job.setAttempts(attempts);
        job.setMaxAttempts(maxAttempts);
        return job;
    }
}
