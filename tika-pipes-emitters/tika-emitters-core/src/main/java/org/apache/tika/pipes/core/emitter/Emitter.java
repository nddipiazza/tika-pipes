package org.apache.tika.pipes.core.emitter;

import org.pf4j.ExtensionPoint;

import java.io.IOException;
import java.util.List;

public interface Emitter extends ExtensionPoint {
    <T extends EmitterConfig> void init(T emitterConfig);
    String getPluginId();
    void emit(List<EmitOutput> emitOutputs) throws IOException;
}
