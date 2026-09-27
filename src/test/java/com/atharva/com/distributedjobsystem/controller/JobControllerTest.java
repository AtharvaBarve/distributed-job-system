package com.atharva.com.distributedjobsystem.controller;

import com.atharva.com.distributedjobsystem.entity.Job;
import com.atharva.com.distributedjobsystem.entity.JobType;
import com.atharva.com.distributedjobsystem.exception.JobNotFoundException;
import com.atharva.com.distributedjobsystem.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobControllerTest {
    private final JobService service = mock(JobService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new JobController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @Test
    void validCreationReturns201() throws Exception {
        when(service.createJob(any())).thenReturn(new Job());
        mvc.perform(post("/api/jobs").contentType("application/json")
                        .content("{\"type\":\"EMAIL\",\"payload\":\"{}\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void missingTypeReturns400() throws Exception {
        mvc.perform(post("/api/jobs").contentType("application/json")
                        .content("{\"payload\":\"{}\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingJobReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getJob(id)).thenThrow(new JobNotFoundException(id));
        mvc.perform(get("/api/jobs/" + id)).andExpect(status().isNotFound());
    }
}
