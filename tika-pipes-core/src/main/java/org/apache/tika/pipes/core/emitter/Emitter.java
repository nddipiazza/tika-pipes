package org.apache.tika.pipes.core.emitter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.pf4j.ExtensionPoint;

public interface Emitter extends ExtensionPoint {
    void emit(EmitterConfig emitterConfig, String emitKey, Map<String, Object> metadata, InputStream is) throws IOException;
}
