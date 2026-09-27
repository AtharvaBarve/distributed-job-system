package com.atharva.com.distributedjobsystem.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class JobMetrics {
    private final Counter submitted;
    private final Counter completed;
    private final Counter failed;
    private final Counter retried;
    private final Timer processingDuration;
    private final AtomicInteger processing = new AtomicInteger();

    public JobMetrics(MeterRegistry registry) {
        submitted = registry.counter("jobs.submitted");
        completed = registry.counter("jobs.completed");
        failed = registry.counter("jobs.failed");
        retried = registry.counter("jobs.retried");
        processingDuration = registry.timer("jobs.processing.duration");
        registry.gauge("jobs.processing.current", processing);
    }

    public void submitted() { submitted.increment(); }
    public void completed() { completed.increment(); }
    public void failed() { failed.increment(); }
    public void retried() { retried.increment(); }
    public void started() { processing.incrementAndGet(); }
    public void stopped() { processing.decrementAndGet(); }
    public Timer.Sample startTimer() { return Timer.start(); }
    public void record(Timer.Sample sample) { sample.stop(processingDuration); }
}
