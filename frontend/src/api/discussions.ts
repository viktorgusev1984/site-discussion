import api from './client'; import type {Category,Discussion,Page,User} from '../types';
export type Filters={q?:string;category?:string;status?:string;sort?:string;page?:number};
export const discussionsApi={list:(filters:Filters)=>api.get<Page<Discussion>>('/discussions',{params:filters}).then(r=>r.data),get:(id:string|number)=>api.get<Discussion>(`/discussions/${id}`).then(r=>r.data),save:(data:Partial<Discussion>,id?:string)=>api.request<Discussion>({url:id?`/discussions/${id}`:'/discussions',method:id?'put':'post',data}).then(r=>r.data),remove:(id:number)=>api.delete(`/discussions/${id}`),vote:(id:number,active:boolean)=>api.request({url:`/discussions/${id}/vote`,method:active?'put':'delete'}).then(r=>r.data),commentVote:(id:number,active:boolean)=>api.request({url:`/comments/${id}/vote`,method:active?'put':'delete'}).then(r=>r.data),react:(target:'discussions'|'comments',id:number,emoji:string,active:boolean)=>api.request({url:`/${target}/${id}/reactions/${encodeURIComponent(emoji)}`,method:active?'put':'delete'}).then(r=>r.data),comment:(id:number,body:string,parentId?:number)=>api.post(`/discussions/${id}/comments`,{body,parentId}).then(r=>r.data),categories:()=>api.get<Category[]>('/categories').then(r=>r.data)};
export const authApi={login:(data:{username:string;password:string})=>api.post('/auth/login',data).then(r=>r.data),register:(data:Record<string,string>)=>api.post('/auth/register',data).then(r=>r.data),profile:(username:string)=>api.get<User&{discussions:Discussion[]}>(`/users/${username}`).then(r=>r.data)};

export async function uploadAttachment(file:File){
  const form=new FormData();form.append('file',file);
  return api.post<{url:string;name:string}>('/attachments',form,{headers:{'Content-Type':undefined}}).then(response=>response.data);
}
