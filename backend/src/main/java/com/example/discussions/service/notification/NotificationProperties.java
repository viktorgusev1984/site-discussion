package com.example.discussions.service.notification;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.notifications")
public record NotificationProperties(
    String encryptionKey,
    Duration workerInterval,
    Integer maxAttempts,
    Duration initialBackoff,
    Duration connectTimeout,
    Duration readTimeout,
    Integer maxResponseBytes,
    Integer maxRedirects) {
  public Duration workerInterval() { return workerInterval == null ? Duration.ofSeconds(10) : workerInterval; }
  public int attempts() { return maxAttempts == null ? 5 : maxAttempts; }
  public Duration initialBackoff() { return initialBackoff == null ? Duration.ofSeconds(30) : initialBackoff; }
  public Duration connectTimeout() { return connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout; }
  public Duration readTimeout() { return readTimeout == null ? Duration.ofSeconds(5) : readTimeout; }
  public int responseLimit() { return maxResponseBytes == null ? 65_536 : maxResponseBytes; }
  public int redirects() { return maxRedirects == null ? 2 : maxRedirects; }
}
