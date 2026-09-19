import {useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {WebShellWithProviders} from '@qwen-code/web-shell';
import {aiApi} from '../api/discussions';

export default function AiAssistantPage(){
  const [sessionId,setSessionId]=useState(''),[error,setError]=useState('');
  useEffect(()=>{aiApi.chatSession().then(result=>setSessionId(result.sessionId)).catch(()=>setError('Не удалось открыть диалог с AI-помощником'))},[]);
  if(error)return <main className="agent-page"><Link to="/">← Обсуждения</Link><p className="error" role="alert">{error}</p></main>;
  if(!sessionId)return <main className="agent-page"><p role="status">Подключаем AI-помощника…</p></main>;
  return <main className="agent-page"><div className="agent-page-heading"><div><Link to="/">← Обсуждения</Link><h1>AI-помощник</h1><p>Продолжайте диалог в персональной сессии Qwen Code.</p></div></div><div className="agent-shell"><WebShellWithProviders baseUrl="/api/agent" token={localStorage.getItem('token')||''} sessionId={sessionId} sessionContext={{kind:'standalone'}} sidebar={false} language="en" brand={{name:'Open Ideas AI'}} /></div></main>
}
