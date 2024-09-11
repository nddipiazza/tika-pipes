package org.apache.tika.pipes.fetchers.http;

import java.io.InputStream;
import java.util.Map;

import fetcher.Fetcher;
import fetcher.FetcherConfig;
import org.springframework.stereotype.Component;

@Component
public class HttpFetcher implements Fetcher {
    @Override
    public InputStream fetch(FetcherConfig fetcherConfig, String fetchKey, Map<String, Object> fetchMetadata) {
        return null;
    }
}
