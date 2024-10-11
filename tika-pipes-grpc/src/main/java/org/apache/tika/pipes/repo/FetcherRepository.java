package org.apache.tika.pipes.repo;

import java.util.List;

import org.apache.ignite.springdata.repository.IgniteRepository;
import org.apache.ignite.springdata.repository.config.RepositoryConfig;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Repository;

import org.apache.tika.pipes.fetcher.FetcherConfig;

@Repository
@RepositoryConfig(cacheName = "FetcherCache")
@DependsOn("igniteRepositoryConfiguration")
public interface FetcherRepository extends IgniteRepository<FetcherConfig, String> {
    FetcherConfig findByFetcherId(String fetcherId);
    List<FetcherConfig> findAll();
    void deleteByFetcherId(String fetcherId);
    boolean existsByFetcherId(String fetcherId);
}
