package org.apache.tika.pipes.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.event.ContextualResultListener;
import net.jodah.failsafe.function.CheckedBiConsumer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.pipes.util.CharsetUtil;

public class TikaServerClient implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(TikaServerManager.class);

  private static final ObjectMapper OM = new ObjectMapper();

  public static final int TIKA_SERVER_CLIENT_MAX_RETRIES = Integer.parseInt(StringUtils.defaultIfBlank(System.getenv("TIKA_SERVER_CLIENT_MAX_RETRIES"), "2"));
  public static final int TIKA_SERVER_CLIENT_BACKOFF_DELAY_MS = Integer.parseInt(StringUtils.defaultIfBlank(System.getenv("TIKA_SERVER_CLIENT_BACKOFF_DELAY_MS"), "1000"));
  public static final int TIKA_SERVER_CLIENT_BACKOFF_MAX_DELAY_MS = Integer.parseInt(StringUtils.defaultIfBlank(System.getenv("TIKA_SERVER_CLIENT_BACKOFF_MAX_DELAY_MS"), "60000"));
  public static final double TIKA_SERVER_CLIENT_BACKOFF_DELAY_FACTOR = Double.parseDouble(StringUtils.defaultIfBlank(System.getenv("TIKA_SERVER_CLIENT_BACKOFF_DELAY_FACTOR"), "2"));
  public static final int TIKA_SERVER_CLIENT_RETRY_MAX_DURATION_MS = Integer.parseInt(StringUtils.defaultIfBlank(System.getenv("TIKA_SERVER_CLIENT_RETRY_MAX_DURATION_MS"), "120000"));

  public static final RetryPolicy RETRY_POLICY = new RetryPolicy()
      .withMaxRetries(TIKA_SERVER_CLIENT_MAX_RETRIES)
      .withBackoff(TIKA_SERVER_CLIENT_BACKOFF_DELAY_MS, TIKA_SERVER_CLIENT_BACKOFF_MAX_DELAY_MS, TimeUnit.MILLISECONDS, TIKA_SERVER_CLIENT_BACKOFF_DELAY_FACTOR)
      .withMaxDuration(TIKA_SERVER_CLIENT_RETRY_MAX_DURATION_MS, TimeUnit.MILLISECONDS)
      .retryOn(IOException.class);

  private final CloseableHttpClient httpClient;

  public TikaServerClient(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public TikaServerClient() {
    PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(
        RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http",
                PlainConnectionSocketFactory.getSocketFactory()).build());
    manager.setDefaultMaxPerRoute(TikaServerConstants.HTTPCLIENT_DEFAULT_MAX_PER_ROUTE);
    manager.setMaxTotal(TikaServerConstants.HTTPCLIENT_DEFAULT_MAX_TOTAL);

    HttpClientBuilder builder = HttpClients.custom()
        .setConnectionManager(manager)
        .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectionRequestTimeout(TikaServerConstants.HTTPCLIENT_REQUEST_TIMEOUT)
            .setConnectTimeout(TikaServerConstants.HTTPCLIENT_CONNECT_TIMEOUT)
            .setSocketTimeout(TikaServerConstants.HTTPCLIENT_SOCKET_TIMEOUT)
            .build()
        );
    this.httpClient = builder.build();
  }

  @Override
  public void close() throws Exception {
    httpClient.close();
  }

  public void parse(String id,
                    Optional<String> contentType,
                    String filePath,
                    Metadata metadata,
                    OutputStream outputStream,
                    long maxBytesReturned,
                    long maxMetadataFieldBytes,
                    long maxEmbeddedDocs,
                    long parseTimeoutMs,
                    String tikaServerEndpoint,
                    Consumer<IOException> errorConsumer
  ) {
    HttpPut httpPut = preparePutRequest(contentType, (int)maxBytesReturned, (int)maxEmbeddedDocs, parseTimeoutMs, tikaServerEndpoint);
    Failsafe.with(TikaServerClient.RETRY_POLICY)
        .onFailedAttempt(failedAttemptConsumer(id, httpPut))
        .onSuccess(successConsumer(id))
        .onRetriesExceeded(retriesExceededConsumer(id, metadata, errorConsumer))
        .run(() -> {
          try {
            parseWithRmeta(id, filePath, metadata, outputStream, (int) maxBytesReturned, (int)maxMetadataFieldBytes, httpPut);
          } catch (TikaServerFailedParseException e) {
            log.error("Failed to parse {}", id, e);
            metadata.add("failed_parse", "true");
            errorConsumer.accept(e);
          }
        });
  }

  public void parse(String id,
                    Optional<String> contentType,
                    InputStream is,
                    Metadata metadata,
                    OutputStream outputStream,
                    long maxBytesReturned,
                    long maxMetadataFieldBytes,
                    long maxEmbeddedDocs,
                    long parseTimeoutMs,
                    String tikaServerEndpoint,
                    Consumer<IOException> errorConsumer
  ) {
    HttpPut httpPut = preparePutRequest(contentType, (int)maxBytesReturned, (int)maxEmbeddedDocs, parseTimeoutMs, tikaServerEndpoint);
    Failsafe.with(TikaServerClient.RETRY_POLICY)
        .onFailedAttempt(failedAttemptConsumer(id, httpPut))
        .onSuccess(successConsumer(id))
        .onRetriesExceeded(retriesExceededConsumer(id, metadata, errorConsumer))
        .run(() -> {
          try {
            parseWithRmeta(id, is, metadata, outputStream, (int) maxBytesReturned, (int)maxMetadataFieldBytes, httpPut);
          } catch (TikaServerFailedParseException e) {
            log.error("Failed to parse {}", id, e);
            metadata.add("failed_parse", "true");
            errorConsumer.accept(e);
          }
        });
  }

  private CheckedBiConsumer<Object, Throwable> retriesExceededConsumer(String id, Metadata metadata, Consumer<IOException> errorConsumer) {
    return (result, ioe) -> {
      log.error("Retries exceeded when trying to parse {}", id);
      metadata.add("parse_timeout", "true");
      errorConsumer.accept((IOException) ioe);
    };
  }

  private ContextualResultListener<Object, Throwable> failedAttemptConsumer(String id, HttpPut httpPut) {
    return (result, failure, ctx) -> {
      String headers = Arrays.stream(httpPut.getAllHeaders()).map(header -> header.getName() + "=" + header.getValue()).collect(Collectors.joining(","));
      log.info(
          "Failed attempt logged when parsing resourceName={}. Failure={}, ExecutionCount={}, ElapsedTimeMs={}, StartTime={}, HttpHeaders={}",
          id,
          failure,
          ctx.getExecutions(),
          ctx.getElapsedTime().toMillis(),
          ctx.getStartTime(),
          headers
      );
    };
  }

  /**
   * Log a successful retry.
   * @param id The id we are parsing.
   * @return The consumer.
   */
  private CheckedBiConsumer<Object, ExecutionContext> successConsumer(String id) {
    return (result, ctx) -> {
      if (ctx.getExecutions() <= 1) {
        log.debug("Successfully did parse of {} on first try taking total of {} ms", id, ctx.getElapsedTime().toMillis());
      } else {
        log.info("Retry was successful on parse of {} after execution count {} taking total of {} ms", id, ctx.getExecutions(), ctx.getElapsedTime().toMillis());
      }
    };
  }

  private void parseWithRmeta(String id, String filePath, Metadata metadata,
                              OutputStream outputStream, int maxBytesReturned, int maxMetadataFieldBytes, HttpPut httpPut) throws IOException {
    File file = new File(filePath);
    httpPut.setHeader("fileUrl", file.toURI().toURL().toString());
    parseRmetaImpl(id, metadata, outputStream, maxBytesReturned, maxMetadataFieldBytes, httpPut);
  }

  private void parseWithRmeta(String id, InputStream is, Metadata metadata,
                              OutputStream outputStream, int maxBytesReturned, int maxMetadataFieldBytes, HttpPut httpPut) throws IOException {
    httpPut.setEntity(new InputStreamEntity(is));
    parseRmetaImpl(id, metadata, outputStream, maxBytesReturned, maxMetadataFieldBytes, httpPut);
  }

  /**
   * This does the actual parse from Tika Server by sending the file to Tika Server's /rmeta endpoint.
   * It will write the bytes of the document itself and embedded documents.
   * @param maxBytesReturned If != -1, maximum number of total bytes (including embedded documents) to return.
   * @param maxMetadataFieldBytes If != -1, will single metadata fields will be trimmed to max of this number.
   */
  private void parseRmetaImpl(String id, Metadata metadata, OutputStream outputStream, int maxBytesReturned, int maxMetadataFieldBytes, HttpPut httpPut) throws IOException {
    CloseableHttpResponse response = httpClient.execute(httpPut);
    try {
      if (response.getStatusLine().getStatusCode()!= 200) {
        throw new TikaServerFailedParseException(response.getStatusLine().getStatusCode(), IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
      }
      AtomicInteger bytesAlreadyRead = new AtomicInteger(0);

      JsonFactory jsonFactory = OM.getFactory();
      JsonParser jsonParser = jsonFactory.createParser(response.getEntity().getContent());
      JsonToken arrayStartToken = jsonParser.nextToken();
      if (arrayStartToken != JsonToken.START_ARRAY) {
        throw new IllegalStateException("The first element of the Json structure was expected to be a start array token, but it was: " + arrayStartToken);
      }

      JsonToken nextToken = jsonParser.nextToken();
      AtomicBoolean firstFile = new AtomicBoolean(true);
      while (nextToken != JsonToken.END_ARRAY) {
        nextToken = parseNextField(jsonParser, outputStream, metadata, maxBytesReturned, maxMetadataFieldBytes, bytesAlreadyRead, firstFile);
      }
    } finally {
      HttpClientUtils.closeQuietly(response);
    }
  }

  public Optional<String> getTikaExceptionStack(Metadata metadata) {
    String ex = metadata.get("X-TIKA:EXCEPTION:runtime");
    if (StringUtils.isNotBlank(ex)) {
      return Optional.of(ex);
    }
    return Optional.empty();
  }

  private HttpPut preparePutRequest(Optional<String> contentType, int maxBytesReturned, int maxEmbeddedDocs,
                                    long parseTimeoutMs, String nextEndpoint) {
    HttpPut putRequest = new HttpPut(nextEndpoint + "/rmeta/text");
    if (contentType.isPresent()) {
      putRequest.setHeader("Content-Type", contentType.get());
    }
    if (maxEmbeddedDocs > -1) {
      putRequest.setHeader("maxEmbeddedResources", String.valueOf(maxEmbeddedDocs));
    }
    if (maxBytesReturned > -1) {
      putRequest.setHeader("writeLimit", String.valueOf(maxBytesReturned));
    }
    if (parseTimeoutMs > -1) {
      putRequest.setHeader("maxParseTime", String.valueOf(parseTimeoutMs));
    }
    RequestConfig requestConfig = RequestConfig.custom()
        .setSocketTimeout(TikaServerConstants.TIKA_SERVER_CLIENT_CONNECT_TIMEOUT)
        .setConnectTimeout(TikaServerConstants.TIKA_SERVER_CLIENT_CONNECT_TIMEOUT)
        .setConnectionRequestTimeout(TikaServerConstants.TIKA_SERVER_CLIENT_CONNECT_TIMEOUT)
        .build();

    putRequest.setConfig(requestConfig);

    return putRequest;
  }

  private JsonToken parseNextField(JsonParser jsonParser, OutputStream os, Metadata metadata, int maxBytesReturned, int maxMetadataFieldBytes, AtomicInteger bytesAlreadyRead, AtomicBoolean firstFile) throws IOException {
    String nextFieldName = jsonParser.nextFieldName();
    if (nextFieldName == null) {
      firstFile.set(false);
      return jsonParser.nextToken();
    }
    JsonToken nextToken = jsonParser.nextToken();
    if (nextToken == JsonToken.START_ARRAY) {
      List<String> list = jsonParser.readValueAs(new TypeReference<List<String>>() {
      });
      if (maxMetadataFieldBytes >= 0) {
        list = list.stream().map(str -> StringUtils.truncate(str, maxMetadataFieldBytes)).collect(Collectors.toList());
      }
      metadata.set(Property.externalText(nextFieldName), list.toArray(new String[] {}));
    } else if ("X-TIKA:content".equals(nextFieldName)) {
      IOUtils.write(jsonParser.getText(), os, CharsetUtil.bestCharset(jsonParser.getText().getBytes()));
    } else if (firstFile.get()) {
      String metadataVal = jsonParser.getText();
      if (maxMetadataFieldBytes >= 0) {
        metadataVal = StringUtils.truncate(metadataVal, maxMetadataFieldBytes);
      }
      metadata.set(nextFieldName, metadataVal);
    }
    return nextToken;
  }

  public String getTikaServerEndpoint(String tikaServerManagerUrl) throws IOException {
    String endpoint = tikaServerManagerUrl + "/?action=endpoint";
    HttpGet httpGet = new HttpGet(endpoint);
    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
      String body = IOUtils.toString(response.getEntity().getContent(),
          Charset.defaultCharset());
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Could not get a tika server manager endpoint - got status code " + response.getStatusLine() + ", text: " + body);
      }
      return StringUtils.trim(body);
    }
  }

  public List getStatus(String tikaServerManagerUrl) throws IOException {
    String endpoint = tikaServerManagerUrl + "/?action=status";
    HttpGet httpGet = new HttpGet(endpoint);
    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
      String body = IOUtils.toString(response.getEntity().getContent(),
          Charset.defaultCharset());
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException(
            "Could not get a tika server manager endpoint - got status code " + response.getStatusLine() + ", text: " +
                body);
      }
      return new Gson().fromJson(body, List.class);
    }
  }
}
