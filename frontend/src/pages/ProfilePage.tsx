import {useEffect,useState} from 'react';
import {useParams} from 'react-router-dom';
import {authApi} from '../api/discussions';
import {useAuth} from '../auth/AuthContext';
import type {Discussion,User} from '../types';
import DiscussionList from '../components/DiscussionList';
import UserAvatar from '../components/UserAvatar';

type Profile=User&{discussions:Discussion[]};

export default function ProfilePage(){
  const {username=''}=useParams();
  const {user}=useAuth();
  const [profile,setProfile]=useState<Profile>();
  const [error,setError]=useState('');

  useEffect(()=>{
    let active=true;
    setProfile(undefined);
    setError('');
    authApi.profile(username)
      .then(result=>{if(active)setProfile(result)})
      .catch(()=>{if(active)setError('Не удалось загрузить профиль пользователя')});
    return()=>{active=false};
  },[username]);

  if(error)return <main className="profile-state state" role="alert">{error}</main>;
  if(!profile)return <main className="profile-state state">Загрузка…</main>;

  async function setRole(role:'USER'|'MODERATOR'){
    const updated=await authApi.setRole(profile!.username,role);
    setProfile(current=>current&&({...current,role:updated.role}));
  }

  const roleLabel=profile.role==='MODERATOR'?'Модератор':profile.role==='ADMIN'?'Администратор':'Участник';
  const joinedAt=profile.createdAt?new Date(profile.createdAt).toLocaleDateString('ru'):null;

  return <main className="profile">
    <section className="profile-summary" aria-labelledby="profile-name">
      <UserAvatar user={profile} className="profile-avatar"/>
      <h1 id="profile-name">{profile.displayName||profile.username}</h1>
      <b className="profile-username">@{profile.username}</b>
      <p>{profile.bio||'Участник сообщества'}</p>
      <span className="role-badge">{roleLabel}</span>
      {joinedAt&&<small>На платформе с {joinedAt}</small>}
      {user?.role==='ADMIN'&&user.id!==profile.id&&profile.role!=='ADMIN'&&<div className="role-controls">
        {profile.role==='MODERATOR'
          ?<button className="secondary" onClick={()=>setRole('USER')}>Снять модератора</button>
          :<button className="primary" onClick={()=>setRole('MODERATOR')}>Назначить модератором</button>}
      </div>}
    </section>
    <section className="profile-discussions" aria-labelledby="profile-discussions-title">
      <div className="profile-discussions-heading">
        <h2 id="profile-discussions-title">Идеи пользователя</h2>
        <span>{profile.discussions.length}</span>
      </div>
      {profile.discussions.length
        ?<DiscussionList items={profile.discussions}/>
        :<div className="state">Пользователь пока не опубликовал ни одной идеи</div>}
    </section>
  </main>;
}
