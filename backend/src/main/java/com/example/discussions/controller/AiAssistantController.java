package com.example.discussions.controller;

import com.example.discussions.exception.ApiException;
import com.example.discussions.repository.DiscussionRepository;
import com.example.discussions.repository.AiChatSessionRepository;
import com.example.discussions.model.AiChatSession;
import com.example.discussions.service.CurrentUser;
import com.example.discussions.service.ai.AiAssistantService;
import com.example.discussions.service.ai.QwenAgentClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {
  public record ImproveRequest(@NotBlank @Size(max=200) String title,
      @NotBlank @Size(max=50000) String body) {}
  public record ReplyRequest(@Size(max=1000) String instruction) {}
  public record AiResult(String text) {}
  public record ChatSession(String sessionId) {}

  private final AiAssistantService assistant;
  private final DiscussionRepository discussions;
  private final AiChatSessionRepository chatSessions;
  private final CurrentUser current;
  private final QwenAgentClient agent;

  public AiAssistantController(AiAssistantService assistant, DiscussionRepository discussions,
      AiChatSessionRepository chatSessions, CurrentUser current, QwenAgentClient agent) {
    this.assistant = assistant;
    this.discussions = discussions;
    this.chatSessions = chatSessions;
    this.current = current;
    this.agent = agent;
  }

  @PostMapping("/chat/session")
  public ChatSession chatSession() {
    var user = current.required();
    return chatSessions.findFirstByUserOrderByCreatedAtDesc(user)
        .map(session -> new ChatSession(session.sessionId))
        .orElseGet(() -> {
          var session = new AiChatSession();
          session.user = user;
          session.sessionId = agent.createSession();
          chatSessions.save(session);
          return new ChatSession(session.sessionId);
        });
  }

  @PostMapping("/drafts/improve")
  public AiResult improve(@Valid @RequestBody ImproveRequest input) {
    return new AiResult(assistant.improve(input.title(), input.body()));
  }

  @PostMapping("/discussions/{id}/summary")
  public AiResult summary(@PathVariable Long id) {
    var discussion = discussions.findWithCommentsById(id)
        .orElseThrow(() -> ApiException.notFound("Обсуждение не найдено"));
    return new AiResult(assistant.summary(discussion));
  }

  @PostMapping("/discussions/{id}/reply-draft")
  public AiResult reply(@PathVariable Long id, @Valid @RequestBody ReplyRequest input) {
    return new AiResult(assistant.replyDraft(find(id), input.instruction() == null ? "" : input.instruction()));
  }

  private com.example.discussions.model.Discussion find(Long id) {
    return discussions.findById(id).orElseThrow(() -> ApiException.notFound("Обсуждение не найдено"));
  }
}
