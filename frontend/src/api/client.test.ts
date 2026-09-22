import {afterEach,describe,expect,it} from 'vitest';
import api,{apiError} from './client';

const originalAdapter=api.defaults.adapter;

function failWith(response:unknown){
  api.defaults.adapter=async()=>{throw Object.assign(new Error('request failed'),{response})};
}

describe('api client',()=>{
  afterEach(()=>{api.defaults.adapter=originalAdapter});

  it('prefers the reason reported by the API over the fallback',()=>{
    expect(apiError({response:{data:{error:'Обсуждение не найдено'}}},'AI-помощник временно недоступен'))
      .toBe('Обсуждение не найдено');
  });

  it('falls back when the failure carries no usable reason',()=>{
    expect(apiError(new Error('network down'),'AI-помощник временно недоступен'))
      .toBe('AI-помощник временно недоступен');
    expect(apiError({response:{data:{error:'   '}}},'AI-помощник временно недоступен'))
      .toBe('AI-помощник временно недоступен');
  });

  it('announces an expired session when the API answers 401',async()=>{
    failWith({status:401,data:{error:'Сессия истекла. Войдите снова'}});
    const announcements:string[]=[];
    const listener=()=>announcements.push('auth:expired');
    window.addEventListener('auth:expired',listener);

    await expect(api.get('/ai/discussions/1/summary')).rejects.toThrow();
    window.removeEventListener('auth:expired',listener);

    expect(announcements).toEqual(['auth:expired']);
  });

  it('keeps the session when the API fails for another reason',async()=>{
    failWith({status:502,data:{error:'AI-помощник временно недоступен'}});
    let announced=false;
    const listener=()=>{announced=true};
    window.addEventListener('auth:expired',listener);

    await expect(api.get('/ai/discussions/1/summary')).rejects.toThrow();
    window.removeEventListener('auth:expired',listener);

    expect(announced).toBe(false);
  });
});
