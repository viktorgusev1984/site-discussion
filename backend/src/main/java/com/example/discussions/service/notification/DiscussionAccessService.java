package com.example.discussions.service.notification;

import com.example.discussions.model.Discussion;
import com.example.discussions.model.User;
import org.springframework.stereotype.Service;

/** The single authorization seam used immediately before an outbox item is created. */
@Service
public class DiscussionAccessService {
  public boolean canView(User recipient, Discussion discussion) {
    // Discussions are public today. Future category/privacy checks must be added here so that
    // both existing and new notification producers cannot bypass them.
    return recipient != null && discussion != null;
  }
}
