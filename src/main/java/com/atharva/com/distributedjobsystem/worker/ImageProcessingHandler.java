package com.atharva.com.distributedjobsystem.worker;

import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobType;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ImageProcessingHandler implements JobHandler {
    private static final Logger log = LoggerFactory.getLogger(ImageProcessingHandler.class);

    @Override
    public JobType getType() {
        return JobType.IMAGE_PROCESSING;
    }

    @Override
    public void execute(Job job) {
        log.info("Processing image for job {}", job.getId());

        // Simulate image processing
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Image processing interrupted", e);
        }

        log.info("Image processed for job {}", job.getId());
    }
}
