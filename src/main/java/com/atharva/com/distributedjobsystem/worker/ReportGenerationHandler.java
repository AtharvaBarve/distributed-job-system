package com.atharva.com.distributedjobsystem.worker;

import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobType;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ReportGenerationHandler implements JobHandler{
    private static final Logger log = LoggerFactory.getLogger(ReportGenerationHandler.class);
    @Override
    public JobType getType() {
        return JobType.REPORT_GENERATION;
    }

    @Override
    public void execute(Job job) {
        log.info("Generating report for job {}", job.getId());

        // Simulate report generation
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Report generation interrupted", e);
        }

        log.info("Report generated for job {}", job.getId());
    }
}
