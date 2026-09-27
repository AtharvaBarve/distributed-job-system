package com.atharva.com.distributedjobsystem.controller;

import com.atharva.com.distributedjobsystem.dto.CreateJobRequest;
import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Job createJob(@Valid @RequestBody CreateJobRequest request) {
        return jobService.createJob(request);
    }

    @GetMapping("/{id}")
    public Job getJob(@PathVariable UUID id) {
        return jobService.getJob(id);
    }
}
