package org.apache.tika.pipes.core.iterators;

import org.pf4j.ExtensionPoint;

import java.io.Serializable;

public interface PipeIteratorConfig extends Serializable, ExtensionPoint {
    String getPluginId();
    PipeIteratorConfig setPluginId(String pluginId);
    String getPipeIteratorId();
    PipeIteratorConfig setPipeIteratorId(String fetcherId);
    String getConfigJson();
    PipeIteratorConfig setConfigJson(String configJson);
}
