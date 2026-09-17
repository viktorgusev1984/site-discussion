import {useState} from 'react';
import ReactMarkdown from 'react-markdown';
import type {Comment} from '../types';
import MarkdownEditor from './MarkdownEditor';
import EngagementActions from './EngagementActions';

export default function CommentThread({comments,onReply}:{comments:Comment[];onReply:(body:string,parentId?:number)=>Promise<void>}){return <div className="thread">{comments.map(comment=><CommentNode key={comment.id} comment={comment} onReply={onReply}/>)}</div>}
function CommentNode({comment,onReply}:{comment:Comment;onReply:(body:string,parentId?:number)=>Promise<void>}){
  const [open,setOpen]=useState(false),[body,setBody]=useState('');
  return <div className="comment"><div className="comment-line"/><div className="comment-head"><span className="avatar">{comment.author.displayName?.[0]||comment.author.username[0]}</span><b>{comment.author.displayName||comment.author.username}</b><time>{new Date(comment.createdAt).toLocaleString('ru')}</time></div><div className="comment-body markdown"><ReactMarkdown>{comment.body}</ReactMarkdown></div><div className="comment-actions"><EngagementActions id={comment.id} target="comments" count={comment.voteCount||0} active={comment.votedByMe} initialReactions={comment.reactions||[]}/><button className="link-button" onClick={()=>setOpen(!open)}>↩ Ответить</button></div>{open&&<form className="reply" onSubmit={async event=>{event.preventDefault();if(body.trim()){await onReply(body,comment.id);setBody('');setOpen(false)}}}><MarkdownEditor value={body} onChange={setBody} label="Ответ" placeholder="Напишите ответ…"/><button className="primary">Ответить</button></form>}{comment.replies?.length>0&&<CommentThread comments={comment.replies} onReply={onReply}/>}</div>
}
