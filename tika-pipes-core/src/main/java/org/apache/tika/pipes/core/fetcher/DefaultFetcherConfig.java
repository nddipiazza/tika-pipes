package org.apache.tika.pipes.core.fetcher;

import java.util.Map;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

public class DefaultFetcherConfig implements FetcherConfig {
    @QuerySqlField(index = true)
    private String pluginId;
    @QuerySqlField(index = true)
    private String fetcherId;
    private Map<String, Object> config;

    public String getPluginId() {
        return pluginId;
    }

    public DefaultFetcherConfig setPluginId(String pluginId) {
        this.pluginId = pluginId;
        return this;
    }

    public String getFetcherId() {
        return fetcherId;
    }

    public DefaultFetcherConfig setFetcherId(String fetcherId) {
        this.fetcherId = fetcherId;
        return this;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public DefaultFetcherConfig setConfig(Map<String, Object> config) {
        this.config = config;
        return this;
    }
}
