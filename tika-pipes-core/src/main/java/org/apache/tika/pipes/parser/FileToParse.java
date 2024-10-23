package org.apache.tika.pipes.parser;

import java.io.File;

import com.google.common.base.MoreObjects;

public class FileToParse {
  File file;
  String id;
  String url;
  String size;
  int failCount;
  private boolean deleteWhenDone;

  public FileToParse(File file, String id, String url, String size, int failCount) {
    this.file = file;
    this.id = id;
    this.url = url;
    this.size = size;
    this.failCount = failCount;
  }

  public FileToParse(File file, String id, String url, String size, int failCount, boolean deleteWhenDone) {
    this.file = file;
    this.id = id;
    this.url = url;
    this.size = size;
    this.failCount = failCount;
    this.deleteWhenDone = deleteWhenDone;
  }

  public File getFile() {
    return file;
  }

  public FileToParse setFile(File file) {
    this.file = file;
    return this;
  }

  public String getId() {
    return id;
  }

  public FileToParse setId(String id) {
    this.id = id;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public FileToParse setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getSize() {
    return size;
  }

  public FileToParse setSize(String size) {
    this.size = size;
    return this;
  }

  public int getFailCount() {
    return failCount;
  }

  public FileToParse setFailCount(int failCount) {
    this.failCount = failCount;
    return this;
  }

  public boolean isDeleteWhenDone() {
    return deleteWhenDone;
  }

  public FileToParse setDeleteWhenDone(boolean deleteWhenDone) {
    this.deleteWhenDone = deleteWhenDone;
    return this;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("file", file)
            .add("id", id)
            .add("url", url)
            .add("size", size)
            .add("failCount", failCount)
            .add("deleteWhenDone", deleteWhenDone)
            .toString();
  }
}
