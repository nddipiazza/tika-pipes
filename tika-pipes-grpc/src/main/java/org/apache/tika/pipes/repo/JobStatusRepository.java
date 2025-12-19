package org.apache.tika.pipes.repo;

import org.apache.ignite.springdata.repository.IgniteRepository;
import org.apache.ignite.springdata.repository.config.RepositoryConfig;
import org.apache.tika.pipes.job.JobStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryConfig(cacheName = "JobStatusCache")
@org.springframework.context.annotation.Profile("ignite")
public interface JobStatusRepository extends IgniteRepository<JobStatus, String>, BaseJobStatusRepository {
    JobStatus findByJobId(String jobId);
    @NotNull
    List<JobStatus> findAll();
}
