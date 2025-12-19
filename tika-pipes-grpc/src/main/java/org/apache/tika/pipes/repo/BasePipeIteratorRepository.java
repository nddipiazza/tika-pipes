package org.apache.tika.pipes.repo;

import org.apache.tika.pipes.core.iterators.DefaultPipeIteratorConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface BasePipeIteratorRepository {
    DefaultPipeIteratorConfig findByPipeIteratorId(String pipeIteratorId);
    @NotNull
    List<DefaultPipeIteratorConfig> findAll();
    void deleteByPipeIteratorId(String pipeIteratorId);
    <S extends DefaultPipeIteratorConfig> S save(String key, S entity);
}
