package com.example.discussions.controller;

import com.example.discussions.dto.Requests.*;
import com.example.discussions.dto.Responses.*;
import com.example.discussions.exception.ApiException;
import com.example.discussions.model.*;
import com.example.discussions.repository.*;
import com.example.discussions.service.CurrentUser;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api") @Transactional
public class DiscussionController {
  private static final Set<String> ALLOWED_EMOJIS = Set.of("👍", "❤️", "🎉", "😄", "😕", "👀");
  private final DiscussionRepository discussions;
  private final CategoryRepository categories;
  private final CommentRepository comments;
  private final VoteRepository votes;
  private final CommentVoteRepository commentVotes;
  private final ReactionRepository reactions;
  private final DiscussionActionRepository actions;
  private final CurrentUser current;

  public DiscussionController(DiscussionRepository d, CategoryRepository c, CommentRepository comments,
      VoteRepository v, CommentVoteRepository commentVotes, ReactionRepository reactions, DiscussionActionRepository actions, CurrentUser current) {
    this.discussions=d; this.categories=c; this.comments=comments; this.votes=v;
    this.commentVotes=commentVotes; this.reactions=reactions; this.actions=actions; this.current=current;
  }
  @GetMapping("/categories") public List<Category> categories(){return categories.findAll();}
  @GetMapping("/discussions") public Page<DiscussionView> list(@RequestParam(required=false)String q,@RequestParam(required=false)String category,@RequestParam(required=false)Discussion.Status status,@RequestParam(defaultValue="activity,desc")String sort,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){String key=sort.startsWith("vote")?"votes":sort.startsWith("created")?"created":"activity";return discussions.search(blank(q),blank(category),status,key,PageRequest.of(page,Math.min(size,100))).map(this::view);}
  @GetMapping("/discussions/{id}") public DiscussionView get(@PathVariable Long id){return view(find(id));}
  @PostMapping("/discussions") @ResponseStatus(HttpStatus.CREATED) public DiscussionView create(@Valid @RequestBody DiscussionInput in){var d=new Discussion();d.author=current.required();apply(d,in);return view(discussions.save(d));}
  @PutMapping("/discussions/{id}") public DiscussionView update(@PathVariable Long id,@Valid @RequestBody DiscussionInput in){var d=find(id);current.ownerOrAdmin(d.author);apply(d,in);return view(discussions.save(d));}
  @DeleteMapping("/discussions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){var d=find(id);current.ownerOrAdmin(d.author);discussions.delete(d);}
  @PostMapping("/discussions/{id}/comments") @ResponseStatus(HttpStatus.CREATED) public Comment comment(@PathVariable Long id,@Valid @RequestBody CommentInput in){var c=new Comment();c.discussion=find(id);c.author=current.required();c.body=in.body();if(in.parentId()!=null)c.parent=comments.findById(in.parentId()).filter(x->x.discussion.id.equals(id)).orElseThrow(()->ApiException.notFound("Родительский комментарий не найден"));c.discussion.updatedAt=Instant.now();return comments.save(c);}
  @PutMapping("/discussions/{id}/vote") public DiscussionView vote(@PathVariable Long id){var d=find(id);var u=current.required();if(votes.findByUserAndDiscussion(u,d).isEmpty()){var v=new Vote();v.user=u;v.discussion=d;votes.saveAndFlush(v);}return view(d);}
  @DeleteMapping("/discussions/{id}/vote") public DiscussionView unvote(@PathVariable Long id){var d=find(id);votes.findByUserAndDiscussion(current.required(),d).ifPresent(votes::delete);votes.flush();return view(d);}
  @PutMapping("/comments/{id}/vote") public CommentView voteComment(@PathVariable Long id){var c=findComment(id);var u=current.required();if(commentVotes.findByUserAndComment(u,c).isEmpty()){var v=new CommentVote();v.user=u;v.comment=c;commentVotes.saveAndFlush(v);}return commentView(c);}
  @DeleteMapping("/comments/{id}/vote") public CommentView unvoteComment(@PathVariable Long id){var c=findComment(id);commentVotes.findByUserAndComment(current.required(),c).ifPresent(commentVotes::delete);commentVotes.flush();return commentView(c);}
  @PutMapping("/discussions/{id}/reactions/{emoji}") public DiscussionView reactDiscussion(@PathVariable Long id,@PathVariable String emoji){var d=find(id);validateEmoji(emoji);var u=current.required();if(reactions.findByUserAndDiscussionAndEmoji(u,d,emoji).isEmpty()){var r=new Reaction();r.user=u;r.discussion=d;r.emoji=emoji;reactions.saveAndFlush(r);}return view(d);}
  @DeleteMapping("/discussions/{id}/reactions/{emoji}") public DiscussionView unreactDiscussion(@PathVariable Long id,@PathVariable String emoji){var d=find(id);reactions.findByUserAndDiscussionAndEmoji(current.required(),d,emoji).ifPresent(reactions::delete);return view(d);}
  @PutMapping("/discussions/{id}/close") public DiscussionView close(@PathVariable Long id){var d=find(id);current.moderator();d.status=Discussion.Status.CLOSED;return view(discussions.save(d));}
  @PutMapping("/discussions/{id}/cancel") public DiscussionView cancel(@PathVariable Long id){var d=find(id);current.ownerOrModerator(d.author);d.status=Discussion.Status.CANCELLED;return view(discussions.save(d));}
  @PostMapping("/discussions/{id}/actions/jira") @ResponseStatus(HttpStatus.CREATED) public DiscussionView addJira(@PathVariable Long id,@Valid @RequestBody JiraActionInput in){var d=find(id);var actor=current.moderator();var action=new DiscussionAction();action.discussion=d;action.actor=actor;action.type=DiscussionAction.Type.JIRA;action.label=in.key();action.url=in.url();actions.save(action);d.actions.add(action);return view(d);}
  @PutMapping("/comments/{id}/reactions/{emoji}") public CommentView reactComment(@PathVariable Long id,@PathVariable String emoji){var c=findComment(id);validateEmoji(emoji);var u=current.required();if(reactions.findByUserAndCommentAndEmoji(u,c,emoji).isEmpty()){var r=new Reaction();r.user=u;r.comment=c;r.emoji=emoji;reactions.saveAndFlush(r);}return commentView(c);}
  @DeleteMapping("/comments/{id}/reactions/{emoji}") public CommentView unreactComment(@PathVariable Long id,@PathVariable String emoji){var c=findComment(id);reactions.findByUserAndCommentAndEmoji(current.required(),c,emoji).ifPresent(reactions::delete);return commentView(c);}

  private Discussion find(Long id){return discussions.findById(id).orElseThrow(()->ApiException.notFound("Обсуждение не найдено"));}
  private Comment findComment(Long id){return comments.findById(id).orElseThrow(()->ApiException.notFound("Комментарий не найден"));}
  private void apply(Discussion d,DiscussionInput in){d.title=in.title();d.body=in.body();d.category=categories.findById(in.categoryId()).orElseThrow(()->ApiException.notFound("Категория не найдена"));}
  private DiscussionView view(Discussion d){var u=current.optional();var roots=d.comments.stream().filter(c->c.parent==null).map(this::commentView).toList();return new DiscussionView(d.id,d.title,d.body,d.status.name(),d.author,d.category,votes.countByDiscussion(d),d.comments.size(),u.flatMap(x->votes.findByUserAndDiscussion(x,d)).isPresent(),reactionViews(reactions.findByDiscussion(d)),d.createdAt,d.updatedAt,roots,d.actions);}
  private CommentView commentView(Comment c){var u=current.optional();return new CommentView(c.id,c.body,c.author,c.createdAt,commentVotes.countByComment(c),u.flatMap(x->commentVotes.findByUserAndComment(x,c)).isPresent(),reactionViews(reactions.findByComment(c)),c.replies.stream().map(this::commentView).toList());}
  private List<ReactionView> reactionViews(List<Reaction> items){var me=current.optional();return items.stream().collect(Collectors.groupingBy(r->r.emoji,LinkedHashMap::new,Collectors.toList())).entrySet().stream().map(e->new ReactionView(e.getKey(),e.getValue().size(),me.map(u->e.getValue().stream().anyMatch(r->r.user.id.equals(u.id))).orElse(false))).toList();}
  private void validateEmoji(String emoji){if(!ALLOWED_EMOJIS.contains(emoji))throw ApiException.badRequest("Недопустимая реакция");}
  private String blank(String s){return s==null||s.isBlank()?null:s;}
}
