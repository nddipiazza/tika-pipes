package org.apache.tika.pipes.repo;

import org.apache.tika.pipes.fetchers.core.DefaultFetcherConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BaseFetcherRepository {
    DefaultFetcherConfig findByFetcherId(String fetcherId);
    @NotNull
    List<DefaultFetcherConfig> findAll();
    void deleteByFetcherId(String fetcherId);
    <S extends DefaultFetcherConfig> S save(String key, S entity);
}
