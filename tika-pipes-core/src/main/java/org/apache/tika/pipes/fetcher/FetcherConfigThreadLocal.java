package org.apache.tika.pipes.fetcher;

public class FetcherConfigThreadLocal {
    private static final ThreadLocal<FetcherConfig> threadLocalConfig = ThreadLocal.withInitial(() -> null);
    public static <T extends FetcherConfig> void setFetcherConfig(T fetcherConfig) {
        threadLocalConfig.set(fetcherConfig);
    }
    public static <T extends FetcherConfig> T getFetcherConfig(Class<T> fetcherConfigClass) {
        return (T) threadLocalConfig.get();
    }
}
