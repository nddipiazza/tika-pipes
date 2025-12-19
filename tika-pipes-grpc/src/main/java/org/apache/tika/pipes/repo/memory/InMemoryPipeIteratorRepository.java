package org.apache.tika.pipes.repo.memory;

import org.apache.tika.pipes.core.iterators.DefaultPipeIteratorConfig;
import org.apache.tika.pipes.repo.BasePipeIteratorRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!ignite")
public class InMemoryPipeIteratorRepository implements BasePipeIteratorRepository {
    private final ConcurrentHashMap<String, DefaultPipeIteratorConfig> store = new ConcurrentHashMap<>();

    @Override
    public DefaultPipeIteratorConfig findByPipeIteratorId(String pipeIteratorId) {
        return store.get(pipeIteratorId);
    }

    @NotNull
    @Override
    public List<DefaultPipeIteratorConfig> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteByPipeIteratorId(String pipeIteratorId) {
        store.remove(pipeIteratorId);
    }

    @Override
    public <S extends DefaultPipeIteratorConfig> S save(String key, S entity) {
        store.put(key, entity);
        return entity;
    }
}
