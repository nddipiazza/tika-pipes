package org.apache.tika.pipes.fetchers.http;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

public class HttpFetcherConfig {
    @Id
    private String fetcherId;
    private String userName;
    private String password;
    private String ntDomain;
    private String authScheme;
    private String proxyHost;
    private Integer proxyPort;
    private Integer maxConnectionsPerRoute = 1000;
    private Integer maxConnections = 2000;
    private Integer requestTimeout = 120000;
    private Integer connectTimeout = 120000;
    private Integer socketTimeout = 120000;
    private Long maxSpoolSize = -1L;
    private Integer maxRedirects = 0;
    private List<String> httpHeaders = new ArrayList<>();
    private List<String> httpRequestHeaders = new ArrayList<>();
    private Long overallTimeout = 120000L;
    private Integer maxErrMsgSize = 10000000;
    private String userAgent;
    private String jwtIssuer;
    private String jwtSubject;
    private int jwtExpiresInSeconds;
    private String jwtSecret;
    private String jwtPrivateKeyBase64;

    public String getFetcherId() {
        return fetcherId;
    }

    public HttpFetcherConfig setFetcherId(String fetcherId) {
        this.fetcherId = fetcherId;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public HttpFetcherConfig setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public HttpFetcherConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getNtDomain() {
        return ntDomain;
    }

    public HttpFetcherConfig setNtDomain(String ntDomain) {
        this.ntDomain = ntDomain;
        return this;
    }

    public String getAuthScheme() {
        return authScheme;
    }

    public HttpFetcherConfig setAuthScheme(String authScheme) {
        this.authScheme = authScheme;
        return this;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public HttpFetcherConfig setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public HttpFetcherConfig setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }

    public Integer getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public HttpFetcherConfig setMaxConnectionsPerRoute(Integer maxConnectionsPerRoute) {
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        return this;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public HttpFetcherConfig setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public HttpFetcherConfig setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public HttpFetcherConfig setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public HttpFetcherConfig setSocketTimeout(Integer socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public Long getMaxSpoolSize() {
        return maxSpoolSize;
    }

    public HttpFetcherConfig setMaxSpoolSize(Long maxSpoolSize) {
        this.maxSpoolSize = maxSpoolSize;
        return this;
    }

    public Integer getMaxRedirects() {
        return maxRedirects;
    }

    public HttpFetcherConfig setMaxRedirects(Integer maxRedirects) {
        this.maxRedirects = maxRedirects;
        return this;
    }

    public List<String> getHttpHeaders() {
        return httpHeaders;
    }

    public HttpFetcherConfig setHttpHeaders(List<String> httpHeaders) {
        this.httpHeaders = httpHeaders;
        return this;
    }

    public List<String> getHttpRequestHeaders() {
        return httpRequestHeaders;
    }

    public HttpFetcherConfig setHttpRequestHeaders(List<String> httpRequestHeaders) {
        this.httpRequestHeaders = httpRequestHeaders;
        return this;
    }

    public Long getOverallTimeout() {
        return overallTimeout;
    }

    public HttpFetcherConfig setOverallTimeout(Long overallTimeout) {
        this.overallTimeout = overallTimeout;
        return this;
    }

    public Integer getMaxErrMsgSize() {
        return maxErrMsgSize;
    }

    public HttpFetcherConfig setMaxErrMsgSize(Integer maxErrMsgSize) {
        this.maxErrMsgSize = maxErrMsgSize;
        return this;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public HttpFetcherConfig setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }

    public HttpFetcherConfig setJwtIssuer(String jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
        return this;
    }

    public String getJwtSubject() {
        return jwtSubject;
    }

    public HttpFetcherConfig setJwtSubject(String jwtSubject) {
        this.jwtSubject = jwtSubject;
        return this;
    }

    public int getJwtExpiresInSeconds() {
        return jwtExpiresInSeconds;
    }

    public HttpFetcherConfig setJwtExpiresInSeconds(int jwtExpiresInSeconds) {
        this.jwtExpiresInSeconds = jwtExpiresInSeconds;
        return this;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public HttpFetcherConfig setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
        return this;
    }

    public String getJwtPrivateKeyBase64() {
        return jwtPrivateKeyBase64;
    }

    public HttpFetcherConfig setJwtPrivateKeyBase64(String jwtPrivateKeyBase64) {
        this.jwtPrivateKeyBase64 = jwtPrivateKeyBase64;
        return this;
    }
}
