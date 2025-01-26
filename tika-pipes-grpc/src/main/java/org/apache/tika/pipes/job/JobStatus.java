package org.apache.tika.pipes.job;

import java.util.concurrent.atomic.AtomicBoolean;

public class JobStatus {
    private final String jobId;
    private final AtomicBoolean isRunning;
    private final AtomicBoolean isCompleted;
    private final AtomicBoolean hasError;

    public JobStatus(String jobId) {
        this.jobId = jobId;
        this.isRunning = new AtomicBoolean(true);
        this.isCompleted = new AtomicBoolean(false);
        this.hasError = new AtomicBoolean(false);
    }

    public String getJobId() {
        return jobId;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public void setRunning(boolean running) {
        isRunning.set(running);
    }

    public boolean isCompleted() {
        return isCompleted.get();
    }

    public void setCompleted(boolean completed) {
        isCompleted.set(completed);
    }

    public boolean hasError() {
        return hasError.get();
    }

    public void setError(boolean error) {
        hasError.set(error);
    }
}
