package org.apache.tika.pipes.repo.memory;

import org.apache.tika.pipes.fetchers.core.DefaultFetcherConfig;
import org.apache.tika.pipes.repo.BaseFetcherRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!ignite")
public class InMemoryFetcherRepository implements BaseFetcherRepository {
    private final ConcurrentHashMap<String, DefaultFetcherConfig> store = new ConcurrentHashMap<>();

    @Override
    public DefaultFetcherConfig findByFetcherId(String fetcherId) {
        return store.get(fetcherId);
    }

    @NotNull
    @Override
    public List<DefaultFetcherConfig> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteByFetcherId(String fetcherId) {
        store.remove(fetcherId);
    }

    @Override
    public <S extends DefaultFetcherConfig> S save(String key, S entity) {
        store.put(key, entity);
        return entity;
    }
}
