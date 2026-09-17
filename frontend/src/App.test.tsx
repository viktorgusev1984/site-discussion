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

  it('shows the user menu with profile and sign out actions',async()=>{
    localStorage.setItem('token','test-token');
    localStorage.setItem('currentUser',JSON.stringify(user));
    render(<App/>);

    expect(screen.getByRole('img',{name:'Аватар: Тестовый пользователь'}).querySelector('svg')).toBeInTheDocument();
    expect(screen.queryByRole('link',{name:/Профиль/})).not.toBeInTheDocument();
    const accountButton=screen.getByRole('button',{name:'Открыть меню пользователя Тестовый пользователь'});
    expect(accountButton).toContainElement(screen.getByRole('img',{name:'Аватар: Тестовый пользователь'}));
    expect(screen.getByRole('link',{name:'Обсуждения'})).toHaveClass('header-discussions');
    await userEvent.click(accountButton);

    expect(screen.getByRole('menu',{name:'Меню пользователя'})).toBeInTheDocument();
    const profile=screen.getByRole('menuitem',{name:/Профиль/});
    expect(profile).toHaveAttribute('href','/users/demo');
    expect(profile).toHaveClass('account-menu-profile');
    expect(screen.queryByRole('link',{name:'Войти'})).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('menuitem',{name:/Выйти/}));
    expect(screen.getByRole('link',{name:'Войти'})).toBeInTheDocument();
    expect(localStorage.getItem('token')).toBeNull();
  });
});
