package com.example.discussions.service.notification;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class SafeWebhookClient {
  private final HttpClient client;
  private final NotificationProperties properties;

  public SafeWebhookClient(NotificationProperties properties) {
    this.properties = properties;
    client = HttpClient.newBuilder().connectTimeout(properties.connectTimeout())
        .followRedirects(HttpClient.Redirect.NEVER).build();
  }

  public void validate(String endpoint) throws IOException {
    validate(URI.create(endpoint));
  }

  private void validate(URI uri) throws IOException {
    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
      throw new IOException("Notification endpoint must be an HTTPS URL without credentials");
    for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
      if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
          || address.isSiteLocalAddress() || address.isMulticastAddress() || isPrivateRange(address) || isCloudMetadata(address))
        throw new IOException("Notification endpoint resolves to a prohibited network");
    }
  }

  private boolean isCloudMetadata(InetAddress address) {
    byte[] b = address.getAddress();
    return b.length == 4 && (b[0] & 255) == 169 && (b[1] & 255) == 254 && (b[2] & 255) == 169 && (b[3] & 255) == 254;
  }

  private boolean isPrivateRange(InetAddress address) {
    byte[] b = address.getAddress();
    if (b.length == 16) return (b[0] & 0xfe) == 0xfc; // IPv6 unique-local fc00::/7.
    int first = b[0] & 255, second = b[1] & 255;
    return first == 10 || (first == 172 && second >= 16 && second <= 31)
        || (first == 192 && second == 168) || (first == 100 && second >= 64 && second <= 127);
  }

  public void post(String endpoint, String body, Map<String, String> headers) throws IOException, InterruptedException {
    URI uri = URI.create(endpoint);
    for (int redirect = 0; ; redirect++) {
      validate(uri); // Deliberately resolve DNS immediately before every request and redirect.
      HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(properties.readTimeout())
          .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
      headers.forEach(request::header);
      HttpResponse<InputStream> response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
      try (InputStream stream = response.body()) {
        if (stream.readNBytes(properties.responseLimit() + 1).length > properties.responseLimit())
          throw new IOException("Notification endpoint response exceeded configured limit");
      }
      if (response.statusCode() >= 300 && response.statusCode() < 400) {
        if (redirect >= properties.redirects()) throw new IOException("Notification endpoint redirect limit exceeded");
        String location = response.headers().firstValue(HttpHeaders.LOCATION).orElseThrow(() -> new IOException("Redirect without location"));
        uri = uri.resolve(location);
        continue;
      }
      if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new IOException("Notification endpoint returned HTTP " + response.statusCode());
      return;
    }
  }
}
