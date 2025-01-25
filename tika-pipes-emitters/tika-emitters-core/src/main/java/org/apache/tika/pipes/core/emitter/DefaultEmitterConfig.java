package org.apache.tika.pipes.core.emitter;

import java.util.Map;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

public class DefaultEmitterConfig implements EmitterConfig {
    @QuerySqlField(index = true)
    private String pluginId;
    @QuerySqlField(index = true)
    private String emitterId;
    private Map<String, Object> config;

    public String getPluginId() {
        return pluginId;
    }

    public DefaultEmitterConfig setPluginId(String pluginId) {
        this.pluginId = pluginId;
        return this;
    }

    public String getEmitterId() {
        return emitterId;
    }

    public DefaultEmitterConfig setEmitterId(String emitterId) {
        this.emitterId = emitterId;
        return this;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public DefaultEmitterConfig setConfig(Map<String, Object> config) {
        this.config = config;
        return this;
    }
}
