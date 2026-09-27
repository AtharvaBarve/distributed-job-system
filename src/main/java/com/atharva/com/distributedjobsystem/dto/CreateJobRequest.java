package com.atharva.com.distributedjobsystem.dto;

import com.atharva.com.distributedjobsystem.entity.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateJobRequest(
        @NotNull
        JobType type,
        @NotBlank(message = "playload must not be blank")
        String payload
) {}