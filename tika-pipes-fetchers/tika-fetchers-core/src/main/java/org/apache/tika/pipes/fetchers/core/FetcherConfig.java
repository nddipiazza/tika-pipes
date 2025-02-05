package org.apache.tika.pipes.fetchers.core;

import org.pf4j.ExtensionPoint;

import java.io.Serializable;

public interface FetcherConfig extends Serializable, ExtensionPoint {
    String getPluginId();
    FetcherConfig setPluginId(String pluginId);
    String getFetcherId();
    FetcherConfig setFetcherId(String fetcherId);
    String getConfigJson();
    FetcherConfig setConfigJson(String config);
}
