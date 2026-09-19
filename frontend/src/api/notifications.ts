import api from './client';
import type {NotificationChannel,NotificationChannelType,NotificationRule,NotificationTrigger} from '../types';

export interface ChannelInput {name:string;type:NotificationChannelType;url?:string;secret?:string;active?:boolean}
export interface RuleInput {trigger:NotificationTrigger;scope:'ALL_DISCUSSIONS'|'DISCUSSION';discussionId:null|number;channelId:number;active:boolean}

export const notificationsApi={
  channels:()=>api.get<NotificationChannel[]>('/me/notifications/channels').then(r=>r.data),
  createChannel:(input:ChannelInput)=>api.post<NotificationChannel>('/me/notifications/channels',input).then(r=>r.data),
  updateChannel:(id:number,input:ChannelInput&{active:boolean})=>api.put<NotificationChannel>(`/me/notifications/channels/${id}`,input).then(r=>r.data),
  disableChannel:(id:number)=>api.patch<NotificationChannel>(`/me/notifications/channels/${id}/disable`).then(r=>r.data),
  deleteChannel:(id:number)=>api.delete(`/me/notifications/channels/${id}`),
  testChannel:(id:number)=>api.post(`/me/notifications/channels/${id}/test`),
  rules:()=>api.get<NotificationRule[]>('/me/notifications/rules').then(r=>r.data),
  createRule:(input:RuleInput)=>api.post<NotificationRule>('/me/notifications/rules',input).then(r=>r.data),
  updateRule:(id:number,input:RuleInput)=>api.put<NotificationRule>(`/me/notifications/rules/${id}`,input).then(r=>r.data),
  deleteRule:(id:number)=>api.delete(`/me/notifications/rules/${id}`),
};
