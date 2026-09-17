import {useEffect,useRef,useState} from 'react';
import {BrowserRouter,Link,Route,Routes} from 'react-router-dom';
import DiscussionsPage from './pages/DiscussionsPage';
import DiscussionPage from './pages/DiscussionPage';
import EditDiscussionPage from './pages/EditDiscussionPage';
import AuthPage from './pages/AuthPage';
import ProfilePage from './pages/ProfilePage';
import {AuthProvider,useAuth} from './auth/AuthContext';
import './styles.css';

function DefaultAvatar(){
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 2c-4.42 0-8 2.24-8 5v1h16v-1c0-2.76-3.58-5-8-5Z"/></svg>;
}

function UserMenu(){
  const {user,signOut}=useAuth();
  const [open,setOpen]=useState(false);
  const menuRef=useRef<HTMLDivElement>(null);

  useEffect(()=>{
    if(!open)return;
    const close=(event:MouseEvent)=>{if(!menuRef.current?.contains(event.target as Node))setOpen(false)};
    const closeOnEscape=(event:KeyboardEvent)=>{if(event.key==='Escape')setOpen(false)};
    document.addEventListener('mousedown',close);
    document.addEventListener('keydown',closeOnEscape);
    return ()=>{document.removeEventListener('mousedown',close);document.removeEventListener('keydown',closeOnEscape)};
  },[open]);

  if(!user)return <><Link to="/login">Войти</Link><Link className="header-cta" to="/register">Регистрация</Link></>;

  return <div className="account-menu" ref={menuRef}>
    <button className="account-trigger" type="button" aria-expanded={open} aria-haspopup="menu" onClick={()=>setOpen(value=>!value)}>
      <span className="header-avatar"><DefaultAvatar/></span>
      <span className="account-name">{user.displayName||user.username}</span>
      <span className="menu-chevron" aria-hidden="true">▾</span>
    </button>
    {open&&<div className="account-dropdown" role="menu">
      <div className="account-summary"><strong>{user.displayName||user.username}</strong><span>@{user.username}</span></div>
      <Link role="menuitem" to={`/users/${user.username}`} onClick={()=>setOpen(false)}>Профиль</Link>
      <button role="menuitem" type="button" onClick={()=>{setOpen(false);signOut()}}>Выйти</button>
    </div>}
  </div>;
}

function Header(){return <header><Link className="brand" to="/"><span>◉</span> Open Ideas</Link><nav><Link to="/">Обсуждения</Link><UserMenu/></nav></header>}

export default function App(){return <BrowserRouter><AuthProvider><Header/><Routes><Route path="/" element={<DiscussionsPage/>}/><Route path="/discussions/new" element={<EditDiscussionPage/>}/><Route path="/discussions/:id" element={<DiscussionPage/>}/><Route path="/discussions/:id/edit" element={<EditDiscussionPage/>}/><Route path="/users/:username" element={<ProfilePage/>}/><Route path="/login" element={<AuthPage/>}/><Route path="/register" element={<AuthPage register/>}/></Routes></AuthProvider></BrowserRouter>}
