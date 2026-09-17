package com.example.discussions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.discussions.model.Category;
import com.example.discussions.model.Discussion;
import com.example.discussions.model.User;
import com.example.discussions.repository.CategoryRepository;
import com.example.discussions.repository.DiscussionRepository;
import com.example.discussions.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DiscussionRepositoryTest {
    @Autowired DiscussionRepository discussions;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;

    @Test
    void newlyCreatedDiscussionAppearsInUnfilteredList() {
        var author = new User();
        author.username = "author";
        author.email = "author@example.com";
        author.passwordHash = "hash";
        author.displayName = "Author";
        author = users.save(author);

        var category = new Category();
        category.name = "Ideas";
        category.slug = "ideas";
        category.description = "Ideas";
        category.color = "#8250df";
        category = categories.save(category);

        var discussion = new Discussion();
        discussion.title = "A new idea";
        discussion.body = "A sufficiently detailed description";
        discussion.author = author;
        discussion.category = category;
        discussions.saveAndFlush(discussion);

        var result = discussions.search(null, null, null, "activity", PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(discussion.id, result.getContent().getFirst().id);
    }
}
