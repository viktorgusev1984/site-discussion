import {FormEvent,useEffect,useState} from 'react';
import {Link,useParams} from 'react-router-dom';
import {discussionsApi} from '../api/discussions';
import {useAuth} from '../auth/AuthContext';
import type {Discussion} from '../types';
import EngagementActions from '../components/EngagementActions';
import CommentThread from '../components/CommentThread';
import MarkdownEditor from '../components/MarkdownEditor';
import MarkdownContent from '../components/MarkdownContent';

export default function DiscussionPage(){
  const {id=''}=useParams(),{user}=useAuth(),[item,setItem]=useState<Discussion>(),[body,setBody]=useState('');
  const [jiraOpen,setJiraOpen]=useState(false),[jiraKey,setJiraKey]=useState(''),[jiraUrl,setJiraUrl]=useState(''),[error,setError]=useState('');
  const load=()=>discussionsApi.get(id).then(setItem);
  useEffect(()=>{load()},[id]);
  if(!item)return <div className="state">Загрузка…</div>;
  const administrator=user?.role==='ADMIN',moderator=user?.role==='MODERATOR'||administrator,author=user?.id===item.author.id;
  const active=item.status!=='CLOSED'&&item.status!=='CANCELLED';
  async function comment(text:string,parentId?:number){await discussionsApi.comment(item!.id,text,parentId);await load()}
  async function transition(action:'close'|'cancel'){setError('');try{setItem(await discussionsApi[action](item!.id))}catch{setError('Не удалось изменить статус обсуждения')}}
  async function addJira(event:FormEvent){event.preventDefault();setError('');try{setItem(await discussionsApi.addJira(item!.id,jiraKey,jiraUrl));setJiraOpen(false);setJiraKey('');setJiraUrl('')}catch{setError('Проверьте ключ и HTTPS-ссылку задачи Jira')}}
  return <main className="detail"><Link to="/">← Все обсуждения</Link><div className="detail-grid"><article>
    <div className="meta"><span className="category-dot" style={{background:item.category.color}}/>{item.category.name}<span className={`status ${item.status.toLowerCase()}`}>{item.status}</span></div>
    <h1>{item.title}</h1><div className="byline"><span className="avatar">{item.author.displayName[0]}</span>{item.author.displayName} · {new Date(item.createdAt).toLocaleDateString('ru')} {(author||administrator)&&<Link to={`/discussions/${id}/edit`}>Редактировать</Link>}</div>
    {(moderator||(author&&active))&&<div className="discussion-controls">{moderator&&active&&<button className="primary" onClick={()=>transition('close')}>Закрыть обсуждение</button>}{active&&<button className="secondary danger" onClick={()=>transition('cancel')}>Отменить обсуждение</button>}{moderator&&<button className="secondary" onClick={()=>setJiraOpen(open=>!open)}>Добавить задачу Jira</button>}</div>}
    {jiraOpen&&<form className="jira-form" onSubmit={addJira}><label>Ключ задачи<input required maxLength={100} placeholder="PROJ-123" value={jiraKey} onChange={e=>setJiraKey(e.target.value)}/></label><label>Ссылка на задачу<input required type="url" pattern="https://.*" placeholder="https://company.atlassian.net/browse/PROJ-123" value={jiraUrl} onChange={e=>setJiraUrl(e.target.value)}/></label><button className="primary">Сохранить</button></form>}
    {error&&<p className="error" role="alert">{error}</p>}{!!item.actions?.length&&<section className="discussion-actions"><h2>Связанные действия</h2>{item.actions.map(action=><a key={action.id} href={action.url} target="_blank" rel="noreferrer"><b>Jira · {action.label}</b><small>Добавил {action.actor.displayName}</small></a>)}</section>}
    <div className="markdown"><MarkdownContent>{item.body}</MarkdownContent></div><EngagementActions id={item.id} target="discussions" count={item.voteCount} active={item.votedByMe} initialReactions={item.reactions||[]}/><section className="comments-section"><h2>Комментарии <span>{item.commentCount}</span></h2>{user?<form className="new-comment" onSubmit={async e=>{e.preventDefault();if(body.trim()){await comment(body);setBody('')}}}><MarkdownEditor value={body} onChange={setBody} label="Новый комментарий" placeholder="Напишите комментарий…"/><button className="primary">Комментировать</button></form>:<p className="auth-required"><Link to="/login">Войдите</Link>, чтобы оставить комментарий.</p>}<CommentThread comments={item.comments||[]} onReply={comment}/></section>
  </article></div></main>
}
