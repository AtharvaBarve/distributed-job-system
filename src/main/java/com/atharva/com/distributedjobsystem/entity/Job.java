package com.atharva.com.distributedjobsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private int maxAttempts;

    @Version
    private long version;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant startedAt;

    private Instant completedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

}
