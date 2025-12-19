package org.apache.tika.pipes.repo;

import org.apache.tika.pipes.core.emitter.DefaultEmitterConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BaseEmitterRepository {
    DefaultEmitterConfig findByEmitterId(String emitterId);
    @NotNull
    List<DefaultEmitterConfig> findAll();
    void deleteByEmitterId(String emitterId);
    <S extends DefaultEmitterConfig> S save(String key, S entity);
}
