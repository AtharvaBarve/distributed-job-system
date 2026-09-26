package com.atharva.com.distributedjobsystem.worker;

import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobType;
import org.springframework.stereotype.Component;

@Component
public class EmailJobHandler implements JobHandler {

    @Override
    public JobType getType() {
        return JobType.EMAIL;
    }

    @Override
    public void execute(Job job) {
        System.out.println(
                "Sending email for job: " + job.getId()
        );

        // Simulate email processing
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email processing interrupted", e);
        }

        System.out.println(
                "Email sent for job: " + job.getId()
        );
    }
}