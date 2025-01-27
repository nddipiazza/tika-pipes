package org.apache.tika.pipes.core.iterators;

import org.pf4j.ExtensionPoint;

import java.util.List;

public interface PipeIterator extends ExtensionPoint, AutoCloseable {
    String getPipeIteratorId();
    <T extends PipeIteratorConfig> void init(T config);
    boolean hasNext();
    List<PipeInput> next();
}
