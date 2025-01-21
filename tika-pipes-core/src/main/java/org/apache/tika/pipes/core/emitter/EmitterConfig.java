package org.apache.tika.pipes.core.emitter;

import java.util.Map;

import org.pf4j.ExtensionPoint;

public interface EmitterConfig extends ExtensionPoint {
    String getPluginId();
    EmitterConfig setPluginId(String pluginId);
    String getEmitterId();
    EmitterConfig setEmitterId(String fetcherId);
    Map<String, Object> getConfig();
    EmitterConfig setConfig(Map<String, Object> config);
}
