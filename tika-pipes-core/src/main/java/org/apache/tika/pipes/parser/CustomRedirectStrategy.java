package org.apache.tika.pipes.parser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomRedirectStrategy extends LaxRedirectStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(CustomRedirectStrategy.class);
  private Set<String> allowedHosts;

  public CustomRedirectStrategy(Set<String> allowedHosts) {
    this.allowedHosts = allowedHosts;
  }

  @Override
  protected URI createLocationURI(final String location) throws ProtocolException {
    String newLocation = location;
    try {
      new URI(newLocation);
    } catch (final URISyntaxException ex) {
      LOG.warn("Redirected URL: [ " + newLocation + " ] will be encoded");
      newLocation = UrlUtils.encodeURL(newLocation);
    }
    return super.createLocationURI(newLocation);
  }

  @Override
  public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
    boolean isRedirectedSuper = super.isRedirected(request, response, context);
    if (isRedirectedSuper) {
      Header locationHeader = response.getFirstHeader("Location");
      String location = locationHeader.getValue();
      if (StringUtils.isBlank(location)) {
        return false;
      }
      URI uri;
      try {
        uri = new URI(location);
      } catch (URISyntaxException e) {
        return true;
      }
      if (!allowedHosts.isEmpty() && !allowedHosts.contains(uri.getHost())) {
        LOG.info("Not allowing external redirect. OriginalUrl={}," +
            " RedirectLocation={}", request.getRequestLine().getUri(), location);
        return false;
      }
    }
    return isRedirectedSuper;
  }
}
