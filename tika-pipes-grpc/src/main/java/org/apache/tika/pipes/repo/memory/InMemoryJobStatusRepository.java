package org.apache.tika.pipes.repo.memory;

import org.apache.tika.pipes.job.JobStatus;
import org.apache.tika.pipes.repo.BaseJobStatusRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!ignite")
public class InMemoryJobStatusRepository implements BaseJobStatusRepository {
    private final ConcurrentHashMap<String, JobStatus> store = new ConcurrentHashMap<>();

    @Override
    public JobStatus findByJobId(String jobId) {
        return store.get(jobId);
    }

    @NotNull
    @Override
    public List<JobStatus> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public <S extends JobStatus> S save(String key, S entity) {
        store.put(key, entity);
        return entity;
    }
}
