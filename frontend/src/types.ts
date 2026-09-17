export interface User { id:number; username:string; displayName:string; bio?:string; role?:string; createdAt?:string }
export interface Category { id:number; name:string; slug:string; description?:string; color:string }
export interface Comment { id:number; body:string; author:User; createdAt:string; replies:Comment[] }
export interface Discussion { id:number; title:string; body:string; status:string; author:User; category:Category; voteCount:number; commentCount:number; votedByMe?:boolean; createdAt:string; updatedAt:string; comments?:Comment[] }
export interface Page<T> { content:T[]; number:number; totalPages:number; totalElements:number }
