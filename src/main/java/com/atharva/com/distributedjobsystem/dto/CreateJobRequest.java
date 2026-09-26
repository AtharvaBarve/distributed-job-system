package com.atharva.com.distributedjobsystem.dto;

import com.atharva.com.distributedjobsystem.entity.JobType;
import jakarta.validation.constraints.NotNull;

public record CreateJobRequest(
        @NotNull
        JobType type,

        String payload
) {
}