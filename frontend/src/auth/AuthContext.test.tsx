import {act,render,screen} from '@testing-library/react';
import {beforeEach,describe,expect,it} from 'vitest';
import {AuthProvider,useAuth} from './AuthContext';

function SessionProbe(){const {user}=useAuth();return <p>{user?`user:${user.username}`:'guest'}</p>}

const storedUser={id:1,username:'demo',displayName:'Демо пользователь'};
const jwt=(exp:number)=>`header.${btoa(JSON.stringify({sub:'demo',exp}))}.signature`;
const expiredJwt=jwt(Math.floor(Date.now()/1000)-60);
const liveJwt=jwt(Math.floor(Date.now()/1000)+3600);

function signInWith(token:string){
  localStorage.setItem('token',token);
  localStorage.setItem('currentUser',JSON.stringify(storedUser));
}

describe('session lifetime',()=>{
  beforeEach(()=>localStorage.clear());

  it('treats a stored session with an expired token as signed out',()=>{
    signInWith(expiredJwt);
    render(<AuthProvider><SessionProbe/></AuthProvider>);

    expect(screen.getByText('guest')).toBeInTheDocument();
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('currentUser')).toBeNull();
  });

  it('keeps a session whose token has not expired yet',()=>{
    signInWith(liveJwt);
    render(<AuthProvider><SessionProbe/></AuthProvider>);

    expect(screen.getByText('user:demo')).toBeInTheDocument();
  });

  it('signs the user out when the API reports an expired session',()=>{
    signInWith(liveJwt);
    render(<AuthProvider><SessionProbe/></AuthProvider>);
    expect(screen.getByText('user:demo')).toBeInTheDocument();

    act(()=>{window.dispatchEvent(new Event('auth:expired'))});

    expect(screen.getByText('guest')).toBeInTheDocument();
    expect(localStorage.getItem('token')).toBeNull();
  });
});
