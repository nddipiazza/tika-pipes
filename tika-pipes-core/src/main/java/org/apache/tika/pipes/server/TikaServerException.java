package org.apache.tika.pipes.server;

public class TikaServerException extends RuntimeException {
  public TikaServerException() {
    super();
  }

  public TikaServerException(String message) {
    super(message);
  }

  public TikaServerException(String message, Throwable cause) {
    super(message, cause);
  }

  public TikaServerException(Throwable cause) {
    super(cause);
  }

  protected TikaServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
