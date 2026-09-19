package com.example.discussions.dto;

import com.example.discussions.model.NotificationChannel;
import com.example.discussions.model.NotificationChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class NotificationChannelDtos {
  private NotificationChannelDtos() {}

  public record CreateRequest(
      @NotBlank @Size(max = 100) String name,
      @NotNull NotificationChannelType type,
      @Size(max = 2048)
      @Pattern(regexp = "^(?:https://[^\\s]+)?$", message = "URL должен быть абсолютным HTTPS URL")
      String url,
      @Size(max = 512) String secret) {}

  /** A blank URL or secret on update retains the already stored value. */
  public record UpdateRequest(
      @NotBlank @Size(max = 100) String name,
      @NotNull NotificationChannelType type,
      @Size(max = 2048)
      @Pattern(regexp = "^(?:https://[^\\s]+)?$", message = "URL должен быть абсолютным HTTPS URL")
      String url,
      @Size(max = 512) String secret,
      @NotNull Boolean active) {}

  public record View(Long id, NotificationChannelType type, String name, boolean active,
      String connection, Instant createdAt, Instant lastSuccessfulCheckAt) {
    public static View of(NotificationChannel channel) {
      return new View(channel.id, channel.type, channel.name, channel.active,
          channel.connectionDescription, channel.createdAt, channel.lastSuccessfulCheckAt);
    }
  }
}
