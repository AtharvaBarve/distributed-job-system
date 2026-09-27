package com.atharva.com.distributedjobsystem.repository;

import com.atharva.com.distributedjobsystem.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
public interface JobRepository extends JpaRepository<Job , UUID> {

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
    update Job j
    set j.status = com.atharva.com.distributedjobsystem.entity.JobStatus.PROCESSING,
        j.attempts = j.attempts + 1
    where j.id = :id
      and j.status in (
          com.atharva.com.distributedjobsystem.entity.JobStatus.QUEUED,
          com.atharva.com.distributedjobsystem.entity.JobStatus.RETRYING
      )
""")
    int claimForProcessing(UUID id);

}
