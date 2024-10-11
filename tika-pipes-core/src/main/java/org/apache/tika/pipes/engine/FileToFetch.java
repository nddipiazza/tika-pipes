package org.apache.tika.pipes.engine;

import com.google.common.base.MoreObjects;

public class FileToFetch {
  String id;
  String url;
  String fileSize;
  int failCount;

  public FileToFetch(String id, String url, String fileSize, int failCount) {
    this.id = id;
    this.url = url;
    this.fileSize = fileSize;
    this.failCount = failCount;
  }

  public String getId() {
    return id;
  }

  public FileToFetch setId(String id) {
    this.id = id;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public FileToFetch setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getFileSize() {
    return fileSize;
  }

  public FileToFetch setFileSize(String fileSize) {
    this.fileSize = fileSize;
    return this;
  }

  public int getFailCount() {
    return failCount;
  }

  public FileToFetch setFailCount(int failCount) {
    this.failCount = failCount;
    return this;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("url", url)
        .add("fileSize", fileSize)
        .add("failCount", failCount)
        .toString();
  }
}
