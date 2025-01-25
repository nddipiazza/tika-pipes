package org.apache.tika.pipes.fetchers.core;

import java.io.Serializable;
import java.util.Map;

import org.pf4j.ExtensionPoint;

public interface FetcherConfig extends Serializable, ExtensionPoint {
    String getPluginId();
    FetcherConfig setPluginId(String pluginId);
    String getFetcherId();
    FetcherConfig setFetcherId(String fetcherId);
    Map<String, Object> getConfig();
    FetcherConfig setConfig(Map<String, Object> config);
}
