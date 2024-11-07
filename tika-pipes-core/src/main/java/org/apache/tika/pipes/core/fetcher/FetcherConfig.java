package org.apache.tika.pipes.core.fetcher;

import java.io.Serializable;
import java.util.Map;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

public class FetcherConfig implements Serializable {
    @QuerySqlField(index = true)
    private String pluginId;
    @QuerySqlField(index = true)
    private String fetcherId;
    private Map<String, Object> config;

    public String getPluginId() {
        return pluginId;
    }

    public FetcherConfig setPluginId(String pluginId) {
        this.pluginId = pluginId;
        return this;
    }

    public String getFetcherId() {
        return fetcherId;
    }

    public FetcherConfig setFetcherId(String fetcherId) {
        this.fetcherId = fetcherId;
        return this;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public FetcherConfig setConfig(Map<String, Object> config) {
        this.config = config;
        return this;
    }
}
