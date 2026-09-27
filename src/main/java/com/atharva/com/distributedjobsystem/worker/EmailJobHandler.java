package com.atharva.com.distributedjobsystem.worker;

import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobType;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EmailJobHandler implements JobHandler {
    private static final Logger log = LoggerFactory.getLogger(EmailJobHandler.class);

    @Override
    public JobType getType() {
        return JobType.EMAIL;
    }

    @Override
    public void execute(Job job) {
        log.info("Sending email for job {}", job.getId());

        // Simulate email processing
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email processing interrupted", e);
        }

        log.info("Email sent for job {}", job.getId());
    }
}
