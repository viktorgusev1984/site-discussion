import {useState} from 'react';
import type {User} from '../types';

type UserAvatarProps={user:Pick<User,'displayName'|'username'|'avatarUrl'>;className?:string};

export default function UserAvatar({user,className=''}:UserAvatarProps){
  const [imageFailed,setImageFailed]=useState(false);
  const label=user.displayName||user.username||'Пользователь';

  return <span className={`user-avatar ${className}`.trim()} role="img" aria-label={`Аватар: ${label}`}>
    {user.avatarUrl&&!imageFailed
      ?<img src={user.avatarUrl} alt="" onError={()=>setImageFailed(true)}/>
      :<svg viewBox="0 0 24 24" focusable="false"><path d="M12 12a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9Zm0 2c-5 0-8 2.5-8 5.5 0 .8.7 1.5 1.5 1.5h13c.8 0 1.5-.7 1.5-1.5 0-3-3-5.5-8-5.5Z"/></svg>}
  </span>;
}
