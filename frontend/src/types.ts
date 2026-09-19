export interface User { id:number; username:string; displayName:string; avatarUrl?:string; bio?:string; role?:string; createdAt?:string }
export interface Category { id:number; name:string; slug:string; description?:string; color:string }
export interface Reaction { emoji:string; count:number; reactedByMe:boolean }
export interface Comment { id:number; body:string; author:User; createdAt:string; voteCount:number; votedByMe?:boolean; reactions:Reaction[]; replies:Comment[] }
export interface DiscussionAction { id:number; type:'JIRA'; label:string; url:string; actor:User; createdAt:string }
export interface Discussion { id:number; title:string; body:string; status:string; author:User; category:Category; voteCount:number; commentCount:number; votedByMe?:boolean; reactions:Reaction[]; createdAt:string; updatedAt:string; comments?:Comment[]; actions?:DiscussionAction[] }
export interface Page<T> { content:T[]; number:number; totalPages:number; totalElements:number }

export type NotificationChannelType='EMAIL'|'MATTERMOST'|'WEBHOOK';
export type NotificationTrigger='NEW_DISCUSSION'|'NEW_COMMENT'|'NEW_REPLY'|'FIRST_VOTE'|'NEW_REACTION'|'STATUS_CHANGED'|'JIRA_ACTION_CREATED'|'MENTION';
export interface NotificationChannel { id:number; type:NotificationChannelType; name:string; active:boolean; connection:string; createdAt:string; lastSuccessfulCheckAt:string|null }
export interface NotificationRule { id:number; trigger:NotificationTrigger; scope:'ALL_DISCUSSIONS'|'DISCUSSION'; discussionId:number|null; channelId:number; active:boolean }
