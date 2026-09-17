package com.example.discussions.repository;

import com.example.discussions.model.Discussion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class DiscussionSearchRepositoryImpl implements DiscussionSearchRepository {
    private final EntityManager entityManager;

    public DiscussionSearchRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<Discussion> search(
            String query,
            String category,
            Discussion.Status status,
            String sort,
            Pageable pageable) {
        var builder = entityManager.getCriteriaBuilder();
        var criteria = builder.createQuery(Discussion.class);
        var discussion = criteria.from(Discussion.class);
        criteria.where(predicates(builder, discussion, query, category, status));
        criteria.orderBy(order(builder, discussion, sort));

        var items = entityManager
                .createQuery(criteria)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(items, pageable, count(builder, query, category, status));
    }

    private long count(
            CriteriaBuilder builder,
            String query,
            String category,
            Discussion.Status status) {
        var criteria = builder.createQuery(Long.class);
        var discussion = criteria.from(Discussion.class);
        criteria.select(builder.count(discussion));
        criteria.where(predicates(builder, discussion, query, category, status));
        return entityManager.createQuery(criteria).getSingleResult();
    }

    private Predicate[] predicates(
            CriteriaBuilder builder,
            Root<Discussion> discussion,
            String query,
            String category,
            Discussion.Status status) {
        List<Predicate> predicates = new ArrayList<>();
        if (query != null) {
            var pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
            predicates.add(builder.or(
                    builder.like(builder.lower(discussion.get("title")), pattern),
                    builder.like(builder.lower(discussion.get("body")), pattern)));
        }
        if (category != null) {
            predicates.add(builder.equal(discussion.get("category").get("slug"), category));
        }
        if (status != null) {
            predicates.add(builder.equal(discussion.get("status"), status));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private jakarta.persistence.criteria.Order order(
            CriteriaBuilder builder, Root<Discussion> discussion, String sort) {
        return switch (sort) {
            case "votes" -> builder.desc(builder.size(discussion.get("votes")));
            case "created" -> builder.desc(discussion.get("createdAt"));
            default -> builder.desc(discussion.get("updatedAt"));
        };
    }
}
