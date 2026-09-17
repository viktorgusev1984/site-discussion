package com.example.discussions.repository;

import com.example.discussions.model.Discussion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscussionRepository
        extends JpaRepository<Discussion, Long>, DiscussionSearchRepository {}
