package org.apache.tika.pipes.server;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

public class TikaServerFailedParseException extends IOException {
  private int httpCode;

  public TikaServerFailedParseException(int httpCode, String message) {
    super(String.format("Failed to parse on tika server - got status code %d, error payload: %s",
      httpCode, StringUtils.defaultIfBlank(message, "(none)")));
    this.httpCode = httpCode;
  }

  public int getHttpCode() {
    return httpCode;
  }
}
