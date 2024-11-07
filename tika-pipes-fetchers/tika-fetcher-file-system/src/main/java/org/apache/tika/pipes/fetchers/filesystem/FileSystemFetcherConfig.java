package org.apache.tika.pipes.fetchers.filesystem;

import org.apache.tika.pipes.core.fetcher.FetcherConfig;

public class FileSystemFetcherConfig extends FetcherConfig {
    private String basePath;
    private boolean extractFileSystemMetadata;

    public boolean isExtractFileSystemMetadata() {
        return extractFileSystemMetadata;
    }

    public FileSystemFetcherConfig setExtractFileSystemMetadata(boolean extractFileSystemMetadata) {
        this.extractFileSystemMetadata = extractFileSystemMetadata;
        return this;
    }

    public String getBasePath() {
        return basePath;
    }

    public FileSystemFetcherConfig setBasePath(String basePath) {
        this.basePath = basePath;
        return this;
    }
}
