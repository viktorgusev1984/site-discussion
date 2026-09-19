package com.example.discussions.controller;

import com.example.discussions.dto.NotificationChannelDtos.*;
import com.example.discussions.dto.NotificationRuleDtos;
import com.example.discussions.service.CurrentUser;
import com.example.discussions.service.notification.NotificationSettingsService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/notifications")
public class NotificationSettingsController {
  private final CurrentUser currentUser;
  private final NotificationSettingsService settings;

  public NotificationSettingsController(CurrentUser currentUser, NotificationSettingsService settings) {
    this.currentUser = currentUser;
    this.settings = settings;
  }

  @GetMapping("/channels") public List<View> channels() {
    return settings.channels(currentUser.required());
  }
  @PostMapping("/channels") @ResponseStatus(HttpStatus.CREATED)
  public View create(@Valid @RequestBody CreateRequest input) {
    return settings.create(currentUser.required(), input);
  }
  @PutMapping("/channels/{id}") public View update(@PathVariable Long id,
      @Valid @RequestBody UpdateRequest input) {
    return settings.update(currentUser.required(), id, input);
  }
  @PatchMapping("/channels/{id}/disable") public View disable(@PathVariable Long id) {
    return settings.disable(currentUser.required(), id);
  }
  @DeleteMapping("/channels/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) { settings.deleteChannel(currentUser.required(), id); }
  @PostMapping("/channels/{id}/test") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void test(@PathVariable Long id) { settings.test(currentUser.required(), id); }

  @GetMapping("/rules") public List<NotificationRuleDtos.View> rules() {
    return settings.rules(currentUser.required());
  }
  @PostMapping("/rules") @ResponseStatus(HttpStatus.CREATED)
  public NotificationRuleDtos.View createRule(
      @Valid @RequestBody NotificationRuleDtos.CreateRequest input) {
    return settings.createRule(currentUser.required(), input);
  }
  @PutMapping("/rules/{id}") public NotificationRuleDtos.View updateRule(@PathVariable Long id,
      @Valid @RequestBody NotificationRuleDtos.UpdateRequest input) {
    return settings.updateRule(currentUser.required(), id, input);
  }
  @DeleteMapping("/rules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRule(@PathVariable Long id) { settings.deleteRule(currentUser.required(), id); }
}
