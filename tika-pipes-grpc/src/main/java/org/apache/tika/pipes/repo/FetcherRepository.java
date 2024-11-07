package org.apache.tika.pipes.repo;

import java.util.List;

import org.apache.ignite.springdata.repository.IgniteRepository;
import org.apache.ignite.springdata.repository.config.RepositoryConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;

import org.apache.tika.pipes.core.fetcher.FetcherConfig;

@Repository
@RepositoryConfig(cacheName = "FetcherCache")
public interface FetcherRepository extends IgniteRepository<FetcherConfig, String> {
    FetcherConfig findByFetcherId(String fetcherId);
    @NotNull
    List<FetcherConfig> findAll();
    void deleteByFetcherId(String fetcherId);
    boolean existsByFetcherId(String fetcherId);
}
