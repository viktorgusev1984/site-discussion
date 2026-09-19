package com.example.discussions.service.notification;

import com.example.discussions.model.NotificationChannel;
import com.fasterxml.jackson.databind.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.stereotype.Component;

@Component
public class ChannelSecrets {
  private final ObjectMapper json;
  private final SecretKeySpec key;

  public ChannelSecrets(ObjectMapper json, NotificationProperties properties) {
    this.json = json;
    try {
      String configuredValue = properties.encryptionKey() == null ? "" : properties.encryptionKey();
      byte[] configured;
      try { configured = Base64.getDecoder().decode(configuredValue); }
      catch (IllegalArgumentException ignored) { configured = configuredValue.getBytes(StandardCharsets.UTF_8); }
      this.key = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(configured), "AES");
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Invalid notification encryption key", e);
    }
  }

  public JsonNode decrypt(NotificationChannel channel) throws GeneralSecurityException {
    if (channel.encryptedSecrets == null || channel.encryptedSecrets.length < 13) return json.createObjectNode();
    ByteBuffer value = ByteBuffer.wrap(channel.encryptedSecrets);
    byte[] iv = new byte[12]; value.get(iv);
    byte[] cipherText = new byte[value.remaining()]; value.get(cipherText);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
    try { return json.readTree(cipher.doFinal(cipherText)); }
    catch (java.io.IOException e) { throw new GeneralSecurityException("Invalid encrypted channel secret", e); }
  }

  public byte[] encrypt(JsonNode secrets) throws GeneralSecurityException {
    byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
    byte[] encrypted = cipher.doFinal(secrets.toString().getBytes(StandardCharsets.UTF_8));
    return ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
  }
}
