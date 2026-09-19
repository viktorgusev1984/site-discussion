package com.example.discussions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.discussions.model.Discussion;
import com.example.discussions.service.ai.AiAssistantService;
import com.example.discussions.service.ai.QwenAgentClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiAssistantServiceTest {
  @Test void delegatesEditingToTheIsolatedAgentWithAnUntrustedContentBoundary() {
    QwenAgentClient agent = Mockito.mock(QwenAgentClient.class);
    when(agent.run(contains("<user-content>"))).thenReturn("Улучшенный текст");
    var assistant = new AiAssistantService(agent);

    assertThat(assistant.improve("Новая идея", "Описание идеи пользователя"))
        .isEqualTo("Улучшенный текст");
    verify(agent).run(contains("не используй инструменты"));
  }

  @Test void createsAReplyDraftWithoutPublishingIt() {
    QwenAgentClient agent = Mockito.mock(QwenAgentClient.class);
    when(agent.run(contains("Пожелание пользователя"))).thenReturn("Черновик ответа");
    var discussion = new Discussion();
    discussion.title = "Заголовок";
    discussion.body = "Описание обсуждения";

    assertThat(new AiAssistantService(agent).replyDraft(discussion, "Кратко"))
        .isEqualTo("Черновик ответа");
  }
}
