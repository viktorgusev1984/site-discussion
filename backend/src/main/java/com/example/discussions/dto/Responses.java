package com.example.discussions.dto;

import com.example.discussions.model.Category;
import com.example.discussions.model.Discussion;
import com.example.discussions.model.DiscussionAction;
import com.example.discussions.model.User;
import java.time.Instant;
import java.util.List;

public final class Responses {
  private Responses() {}

  public record Auth(String token, User user) {}
  public record ReactionView(String emoji, long count, boolean reactedByMe) {}
  public record CommentView(Long id, String body, User author, Instant createdAt, long voteCount,
      boolean votedByMe, List<ReactionView> reactions, List<CommentView> replies) {}
  public record DiscussionActionView(Long id, User actor, DiscussionAction.Type type, String label,
      String url, Instant createdAt) {
    public static DiscussionActionView of(DiscussionAction action) {
      return new DiscussionActionView(action.id, action.actor, action.type, action.label, action.url,
          action.createdAt);
    }
  }
  public record DiscussionView(Long id, String title, String body, String status, User author,
      Category category, long voteCount, long commentCount, boolean votedByMe,
      List<ReactionView> reactions, Instant createdAt, Instant updatedAt,
      List<CommentView> comments, List<DiscussionActionView> actions) {
    public static DiscussionView of(Discussion discussion, long votes, boolean mine) {
      return new DiscussionView(discussion.id, discussion.title, discussion.body,
          discussion.status.name(), discussion.author, discussion.category, votes,
          discussion.comments.size(), mine, List.of(), discussion.createdAt, discussion.updatedAt,
          List.of(), actionViews(discussion));
    }
  }
  public record Profile(Long id, String username, String displayName, String bio, String role,
      Instant createdAt, List<DiscussionView> discussions) {}

  public static List<DiscussionActionView> actionViews(Discussion discussion) {
    return discussion.actions.stream().map(DiscussionActionView::of).toList();
  }
}
