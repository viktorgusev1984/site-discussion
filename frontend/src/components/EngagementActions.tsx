import {useEffect,useRef,useState} from 'react';
import {discussionsApi} from '../api/discussions';
import type {Reaction} from '../types';

const EMOJIS=['👍','❤️','🎉','😄','😕','👀'];

export default function EngagementActions({id,target,count,active=false,initialReactions=[]}:{id:number;target:'discussions'|'comments';count:number;active?:boolean;initialReactions?:Reaction[]}){
  const [voted,setVoted]=useState(active),[votes,setVotes]=useState(count),[items,setItems]=useState(initialReactions),[open,setOpen]=useState(false),[busy,setBusy]=useState(false);
  const root=useRef<HTMLDivElement>(null);
  useEffect(()=>{const close=(event:MouseEvent)=>{if(!root.current?.contains(event.target as Node))setOpen(false)};document.addEventListener('mousedown',close);return()=>document.removeEventListener('mousedown',close)},[]);
  async function toggleVote(){if(busy)return;const next=!voted;setBusy(true);setVoted(next);setVotes(value=>value+(next?1:-1));try{target==='discussions'?await discussionsApi.vote(id,next):await discussionsApi.commentVote(id,next)}catch{setVoted(!next);setVotes(value=>value+(next?-1:1))}finally{setBusy(false)}}
  async function toggleReaction(emoji:string){const current=items.find(item=>item.emoji===emoji),next=!current?.reactedByMe;setItems(previous=>{const found=previous.find(item=>item.emoji===emoji);if(!found)return[...previous,{emoji,count:1,reactedByMe:true}];const changed={...found,count:found.count+(next?1:-1),reactedByMe:next};return changed.count?previous.map(item=>item.emoji===emoji?changed:item):previous.filter(item=>item.emoji!==emoji)});setOpen(false);try{await discussionsApi.react(target,id,emoji,next)}catch{setItems(initialReactions)}}
  return <div className="engagement" ref={root}>
    <button type="button" className={`compact-vote ${voted?'active':''}`} aria-label={voted?'Снять голос':'Проголосовать'} aria-pressed={voted} disabled={busy} onClick={toggleVote}><span aria-hidden="true">↑</span><b>{votes}</b></button>
    {items.map(item=><button type="button" key={item.emoji} className={`reaction ${item.reactedByMe?'active':''}`} aria-pressed={item.reactedByMe} onClick={()=>toggleReaction(item.emoji)}><span>{item.emoji}</span><b>{item.count}</b></button>)}
    <div className="reaction-picker-wrap"><button type="button" className="add-reaction" aria-label="Добавить реакцию" aria-expanded={open} onClick={()=>setOpen(value=>!value)}>☺<span className="plus">+</span></button>{open&&<div className="reaction-picker" role="menu" aria-label="Выберите реакцию">{EMOJIS.map(emoji=><button type="button" role="menuitem" key={emoji} onClick={()=>toggleReaction(emoji)}>{emoji}</button>)}</div>}</div>
  </div>
}
