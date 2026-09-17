import {render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach,describe,expect,it,vi} from 'vitest';
import App from './App';
import {discussionsApi} from './api/discussions';

vi.spyOn(discussionsApi,'categories').mockResolvedValue([]);
vi.spyOn(discussionsApi,'list').mockResolvedValue({content:[],number:0,totalPages:0,totalElements:0});

const user={id:1,username:'demo',displayName:'Тестовый пользователь'};

describe('authenticated header',()=>{
  beforeEach(()=>{localStorage.clear();history.pushState({},'', '/')});

  it('shows a profile link instead of login actions and can sign out',async()=>{
    localStorage.setItem('token','test-token');
    localStorage.setItem('currentUser',JSON.stringify(user));
    render(<App/>);

    const profile=screen.getByRole('link',{name:'Открыть профиль'});
    expect(profile).toHaveAttribute('href','/users/demo');
    expect(screen.queryByRole('link',{name:'Войти'})).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button',{name:'Выйти'}));
    expect(screen.getByRole('link',{name:'Войти'})).toBeInTheDocument();
    expect(localStorage.getItem('token')).toBeNull();
  });
});
