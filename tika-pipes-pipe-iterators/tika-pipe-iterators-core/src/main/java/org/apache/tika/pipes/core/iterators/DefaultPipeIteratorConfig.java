package org.apache.tika.pipes.core.iterators;

import java.util.Map;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

public class DefaultPipeIteratorConfig implements PipeIteratorConfig {
    @QuerySqlField(index = true)
    private String pluginId;
    @QuerySqlField(index = true)
    private String pipeIteratorId;
    private Map<String, Object> config;

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public DefaultPipeIteratorConfig setPluginId(String pluginId) {
        this.pluginId = pluginId;
        return this;
    }

    @Override
    public String getPipeIteratorId() {
        return pipeIteratorId;
    }

    @Override
    public DefaultPipeIteratorConfig setPipeIteratorId(String pipeIteratorId) {
        this.pipeIteratorId = pipeIteratorId;
        return this;
    }

    @Override
    public Map<String, Object> getConfig() {
        return config;
    }

    @Override
    public DefaultPipeIteratorConfig setConfig(Map<String, Object> config) {
        this.config = config;
        return this;
    }
}
