# Distributed Job Processing System

A backend system for processing long-running jobs asynchronously using Spring Boot, PostgreSQL, Redis, and concurrent workers.

## Problem

Long-running operations should not block API requests. Instead of executing work directly inside an HTTP request, the system accepts a job, places it in a queue, and lets background workers process it asynchronously.

## Architecture

```text
Client
  |
  | HTTP
  v
Spring Boot API
  |
  +------------------+
  |                  |
  v                  v
PostgreSQL          Redis
(Job State)         (Job Queue)
                       |
                       v
              +----------------+
              | Worker Pool    |
              |                |
              | Worker 1       |
              | Worker 2       |
              | Worker 3       |
              | Worker 4       |
              +----------------+
                       |
                       v
                  Job Handler
                       |
                       v
                  PostgreSQL
