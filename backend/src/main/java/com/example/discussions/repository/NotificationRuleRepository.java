package com.example.discussions.repository;

import com.example.discussions.model.NotificationRule;
import com.example.discussions.model.NotificationTrigger;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface NotificationRuleRepository extends JpaRepository<NotificationRule, Long> {
  List<NotificationRule> findByUserIdOrderByIdAsc(Long userId);
  Optional<NotificationRule> findByIdAndUserId(Long id, Long userId);
  @Query("select r from NotificationRule r join fetch r.channel where r.active = true and r.channel.active = true and r.trigger = :trigger and (r.discussion is null or r.discussion.id = :discussionId)")
  List<NotificationRule> findMatching(@Param("trigger") NotificationTrigger trigger, @Param("discussionId") Long discussionId);

  List<NotificationRule> findByUserIdAndTriggerAndActiveTrue(
      Long userId, NotificationTrigger trigger);

  List<NotificationRule> findByUserIdAndTriggerAndDiscussionIdAndActiveTrue(
      Long userId, NotificationTrigger trigger, Long discussionId);

  /** Includes both rules scoped to this discussion and rules applying to all discussions. */
  @Query("""
      select rule from NotificationRule rule
      where rule.active = true
        and rule.user.id = :userId
        and rule.trigger = :trigger
        and (rule.discussion is null or rule.discussion.id = :discussionId)
      """)
  List<NotificationRule> findActiveForDiscussion(
      @Param("userId") Long userId,
      @Param("trigger") NotificationTrigger trigger,
      @Param("discussionId") Long discussionId);
}
