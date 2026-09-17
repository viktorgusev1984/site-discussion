package com.example.discussions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.discussions.dto.Responses.DiscussionView;
import com.example.discussions.model.Category;
import com.example.discussions.model.Discussion;
import com.example.discussions.model.DiscussionAction;
import com.example.discussions.model.User;
import com.example.discussions.repository.CategoryRepository;
import com.example.discussions.repository.DiscussionActionRepository;
import com.example.discussions.repository.DiscussionRepository;
import com.example.discussions.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:action-serialization;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DiscussionActionSerializationTest {
  @Autowired UserRepository users;
  @Autowired CategoryRepository categories;
  @Autowired DiscussionRepository discussions;
  @Autowired DiscussionActionRepository actions;
  @Autowired TransactionTemplate transactions;
  @Autowired ObjectMapper objectMapper;

  @Test
  void serializesActionsAfterPersistenceSessionIsClosed() {
    var discussionId = transactions.execute(status -> createDiscussionWithAction());

    var view = transactions.execute(status ->
        DiscussionView.of(discussions.findById(discussionId).orElseThrow(), 0, false));

    assertEquals("WEB-42", view.actions().getFirst().label());
    assertDoesNotThrow(() -> objectMapper.writeValueAsString(view));
  }

  private Long createDiscussionWithAction() {
    var author = new User();
    author.username = "action-author";
    author.email = "action-author@example.com";
    author.passwordHash = "hash";
    author.displayName = "Action Author";
    author = users.save(author);

    var category = new Category();
    category.name = "Actions";
    category.slug = "actions";
    category.description = "Action serialization";
    category.color = "#8250df";
    category = categories.save(category);

    var discussion = new Discussion();
    discussion.title = "Track this discussion";
    discussion.body = "This discussion has an external tracking action.";
    discussion.author = author;
    discussion.category = category;
    discussion = discussions.save(discussion);

    var action = new DiscussionAction();
    action.discussion = discussion;
    action.actor = author;
    action.type = DiscussionAction.Type.JIRA;
    action.label = "WEB-42";
    action.url = "https://jira.example.com/browse/WEB-42";
    actions.save(action);
    return discussion.id;
  }
}
