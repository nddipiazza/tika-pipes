package org.apache.tika.pipes.repo;

import org.apache.tika.pipes.job.JobStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BaseJobStatusRepository {
    JobStatus findByJobId(String jobId);
    @NotNull
    List<JobStatus> findAll();
    <S extends JobStatus> S save(String key, S entity);
}
