package com.atharva.com.distributedjobsystem.repository;

import com.atharva.com.distributedjobsystem.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
public interface JobRepository extends JpaRepository<Job , UUID> {

}
