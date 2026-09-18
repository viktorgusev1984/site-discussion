import {useState} from 'react';
import {Link,useLocation,useNavigate} from 'react-router-dom';
import {authApi} from '../api/discussions';
import {useAuth} from '../auth/AuthContext';

const DEMO_ACCOUNTS=[
  {label:'Пользователь',username:'demo',password:'demo12345'},
  {label:'Администратор',username:'admin',password:'admin12345'},
] as const;

export default function AuthPage({register=false}:{register?:boolean}){
  const nav=useNavigate();
  const location=useLocation();
  const {signIn}=useAuth();
  const [form,setForm]=useState({username:'',displayName:'',email:'',password:''});
  const [error,setError]=useState('');
  const [isSubmitting,setIsSubmitting]=useState(false);

  async function submit(e:React.FormEvent){
    e.preventDefault();
    if(isSubmitting)return;

    setError('');
    setIsSubmitting(true);
    try{
      const data=register?await authApi.register(form):await authApi.login(form);
      signIn(data);
      const requestedPath=(location.state as {from?:unknown}|null)?.from;
      const returnTo=typeof requestedPath==='string'&&requestedPath.startsWith('/')&&!requestedPath.startsWith('//')?requestedPath:'/';
      nav(returnTo,{replace:true});
    }catch{
      setError('Не удалось выполнить запрос. Проверьте данные.');
      setIsSubmitting(false);
    }
  }

  function useDemo(username:string,password:string){
    setForm({...form,username,password});
    setError('');
  }

  const submitLabel=register?'Зарегистрироваться':'Войти';

  return <main className="auth-page">
    <div className="auth-logo">◎</div>
    <form className="auth-card" onSubmit={submit} aria-busy={isSubmitting}>
      <h1>{register?'Создать аккаунт':'С возвращением'}</h1>
      <p>{register?'Присоединяйтесь к обсуждению':'Войдите, чтобы голосовать и отвечать'}</p>
      {!register&&<aside className="demo-account">
        <strong>Тестовые аккаунты</strong>
        {DEMO_ACCOUNTS.map(account=><div className="demo-account-row" key={account.username}>
          <span>{account.label}: <code>{account.username}</code> · <code>{account.password}</code></span>
          <button type="button" disabled={isSubmitting} onClick={()=>useDemo(account.username,account.password)}>Войти как {account.label.toLowerCase()}</button>
        </div>)}
      </aside>}
      {register&&<>
        <label>Имя<input value={form.displayName} disabled={isSubmitting} onChange={e=>setForm({...form,displayName:e.target.value})} required/></label>
        <label>Email<input type="email" value={form.email} disabled={isSubmitting} onChange={e=>setForm({...form,email:e.target.value})} required/></label>
      </>}
      <label>Логин<input value={form.username} disabled={isSubmitting} onChange={e=>setForm({...form,username:e.target.value})} required/></label>
      <label>Пароль<input type="password" value={form.password} disabled={isSubmitting} onChange={e=>setForm({...form,password:e.target.value})} minLength={8} required/></label>
      {error&&<div className="error">{error}</div>}
      <button className="primary auth-submit" disabled={isSubmitting}>
        {isSubmitting&&<span className="button-spinner" aria-hidden="true"/>}
        <span>{isSubmitting?'Подождите…':submitLabel}</span>
      </button>
      <small>{register?'Уже есть аккаунт?':'Нет аккаунта?'} <Link to={register?'/login':'/register'} state={location.state}>{register?'Войти':'Регистрация'}</Link></small>
    </form>
  </main>;
}
