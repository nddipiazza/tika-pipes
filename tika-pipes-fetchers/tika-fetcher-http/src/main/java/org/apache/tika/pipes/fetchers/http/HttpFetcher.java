package org.apache.tika.pipes.fetchers.http;

import java.io.InputStream;
import java.util.Map;

import org.springframework.stereotype.Component;

import org.apache.tika.pipes.fetcher.Fetcher;
import org.apache.tika.pipes.fetcher.FetcherConfig;

@Component
public class HttpFetcher implements Fetcher {
    @Override
    public InputStream fetch(FetcherConfig fetcherConfig, String fetchKey, Map<String, Object> fetchMetadata) {
        return null;
    }
}
