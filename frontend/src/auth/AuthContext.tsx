import {createContext,useContext,useEffect,useMemo,useState} from 'react';
import {authApi} from '../api/discussions';
import type {User} from '../types';

type AuthResponse={token:string;user:User};
type AuthContextValue={user:User|null;signIn:(response:AuthResponse)=>void;signOut:()=>void};
const USER_STORAGE_KEY='currentUser';

function tokenPayload(){try{const token=localStorage.getItem('token');if(!token)return null;const encoded=token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');return JSON.parse(atob(encoded)) as {sub?:string;exp?:number}}catch{return null}}
function tokenExpired(){const expiresAt=tokenPayload()?.exp;return typeof expiresAt==='number'&&expiresAt*1000<=Date.now()}
function forgetSession(){localStorage.removeItem('token');localStorage.removeItem(USER_STORAGE_KEY)}
function storedUser(){if(tokenExpired()){forgetSession();return null}try{return JSON.parse(localStorage.getItem(USER_STORAGE_KEY)||'null') as User|null}catch{return null}}

const AuthContext=createContext<AuthContextValue>({user:null,signIn:response=>{localStorage.setItem('token',response.token);localStorage.setItem(USER_STORAGE_KEY,JSON.stringify(response.user))},signOut:()=>{localStorage.removeItem('token');localStorage.removeItem(USER_STORAGE_KEY)}});

export function AuthProvider({children}:{children:React.ReactNode}){
  const [user,setUser]=useState<User|null>(storedUser);
  useEffect(()=>{if(user||!localStorage.getItem('token'))return;const username=tokenPayload()?.sub||null;if(!username)return;authApi.profile(username).then(profile=>{setUser(profile);localStorage.setItem(USER_STORAGE_KEY,JSON.stringify(profile))}).catch(forgetSession)},[user]);
  useEffect(()=>{const onExpired=()=>{forgetSession();setUser(null)};window.addEventListener('auth:expired',onExpired);return()=>window.removeEventListener('auth:expired',onExpired)},[]);
  const value=useMemo<AuthContextValue>(()=>({user,signIn:response=>{localStorage.setItem('token',response.token);localStorage.setItem(USER_STORAGE_KEY,JSON.stringify(response.user));setUser(response.user)},signOut:()=>{forgetSession();setUser(null)}}),[user]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export const useAuth=()=>useContext(AuthContext);
