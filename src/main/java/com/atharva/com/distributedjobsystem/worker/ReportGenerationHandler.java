package com.atharva.com.distributedjobsystem.worker;

import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobType;
import org.springframework.stereotype.Component;

@Component
public class ReportGenerationHandler implements JobHandler{
    @Override
    public JobType getType() {
        return JobType.REPORT_GENERATION;
    }

    @Override
    public void execute(Job job) {
        System.out.println(
                "Generating report for job: " + job.getId()
        );

        // Simulate report generation
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Report generation interrupted", e);
        }

        System.out.println(
                "Report generated for job: " + job.getId()
        );
    }
}
