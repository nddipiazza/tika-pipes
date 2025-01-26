package org.apache.tika.pipes.core.iterators;

import java.util.List;

import org.pf4j.ExtensionPoint;

import org.apache.tika.FetchAndParseRequest;

public interface PipeIterator extends ExtensionPoint, AutoCloseable {
    String getPipeIteratorId();
    boolean hasNext();
    List<FetchAndParseRequest> next();
}
