package com.atharva.com.distributedjobsystem.worker;

import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobType;
import org.springframework.stereotype.Component;

@Component
public class ImageProcessingHandler implements JobHandler {

    @Override
    public JobType getType() {
        return JobType.IMAGE_PROCESSING;
    }

    @Override
    public void execute(Job job) {
        System.out.println(
                "Processing image for job: " + job.getId()
        );

        // Simulate image processing
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Image processing interrupted", e);
        }

        System.out.println(
                "Image processed for job: " + job.getId()
        );
    }
}