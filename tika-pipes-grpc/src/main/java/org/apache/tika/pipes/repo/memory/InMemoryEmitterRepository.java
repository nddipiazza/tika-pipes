package org.apache.tika.pipes.repo.memory;

import org.apache.tika.pipes.core.emitter.DefaultEmitterConfig;
import org.apache.tika.pipes.repo.BaseEmitterRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!ignite")
public class InMemoryEmitterRepository implements BaseEmitterRepository {
    private final ConcurrentHashMap<String, DefaultEmitterConfig> store = new ConcurrentHashMap<>();

    @Override
    public DefaultEmitterConfig findByEmitterId(String emitterId) {
        return store.get(emitterId);
    }

    @NotNull
    @Override
    public List<DefaultEmitterConfig> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteByEmitterId(String emitterId) {
        store.remove(emitterId);
    }

    @Override
    public <S extends DefaultEmitterConfig> S save(String key, S entity) {
        store.put(key, entity);
        return entity;
    }
}
