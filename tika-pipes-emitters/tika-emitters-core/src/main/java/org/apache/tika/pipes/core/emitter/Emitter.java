package org.apache.tika.pipes.core.emitter;

import java.io.IOException;
import java.util.List;

import org.pf4j.ExtensionPoint;

import org.apache.tika.FetchAndParseReply;

public interface Emitter extends ExtensionPoint {
    String getPluginId();

    void emit(EmitterConfig emitterConfig, List<FetchAndParseReply> fetchAndParseReplies)
            throws IOException;
}
