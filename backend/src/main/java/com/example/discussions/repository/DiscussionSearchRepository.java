package com.example.discussions.repository;

import com.example.discussions.model.Discussion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DiscussionSearchRepository {
    Page<Discussion> search(
            String query,
            String category,
            Discussion.Status status,
            String sort,
            Pageable pageable);
}
