package org.apache.tika.pipes.fetchers.filesystem;

import org.pf4j.Extension;

import org.apache.tika.pipes.core.emitter.DefaultEmitterConfig;

@Extension
public class FileSystemEmitterConfig extends DefaultEmitterConfig {
    private String basePath;
    private String fileExtension = "json";
    private String onExists = "exception";
    private boolean prettyPrint = false;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getOnExists() {
        return onExists;
    }

    public void setOnExists(String onExists) {
        this.onExists = onExists;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
}
