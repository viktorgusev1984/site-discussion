package com.example.discussions;

import static org.junit.jupiter.api.Assertions.*;

import com.example.discussions.service.notification.*;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class NotificationSecurityTest {
  private SafeWebhookClient client() {
    return new SafeWebhookClient(new NotificationProperties("", Duration.ofSeconds(10), 3,
        Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1), 1024, 1, null));
  }

  @Test void rejectsNonHttpsEndpoints() {
    assertThrows(IOException.class, () -> client().validate("http://example.com/hook"));
  }

  @Test void rejectsLoopbackAndPrivateNetworks() {
    assertThrows(IOException.class, () -> client().validate("https://localhost/hook"));
    assertThrows(IOException.class, () -> client().validate("https://127.0.0.1/hook"));
    assertThrows(IOException.class, () -> client().validate("https://[::1]/hook"));
    assertThrows(IOException.class, () -> client().validate("https://10.0.0.1/hook"));
    assertThrows(IOException.class, () -> client().validate("https://172.16.0.1/hook"));
    assertThrows(IOException.class, () -> client().validate("https://192.168.1.1/hook"));
    assertThrows(IOException.class, () -> client().validate("https://100.64.0.1/hook"));
    assertThrows(IOException.class, () -> client().validate("https://169.254.1.1/hook"));
    assertThrows(IOException.class, () -> client().validate("https://169.254.169.254/latest/meta-data"));
    assertThrows(IOException.class, () -> client().validate("https://[fd00::1]/hook"));
  }
}
