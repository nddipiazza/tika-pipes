package fetcher;

import java.util.Map;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "fetchers")
public class FetcherConfig {
    private String pluginId;
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
