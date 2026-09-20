package com.example.discussions.repository;

import com.example.discussions.model.Discussion;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscussionRepository
        extends JpaRepository<Discussion, Long>, DiscussionSearchRepository {
    @EntityGraph(attributePaths = {"comments", "comments.author"})
    Optional<Discussion> findWithCommentsById(Long id);
}
