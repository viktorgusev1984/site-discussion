import {render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {afterEach,describe,expect,it,vi} from 'vitest';
import AuthPage from './AuthPage';
import {authApi} from '../api/discussions';

afterEach(()=>vi.restoreAllMocks());

describe('demo login',()=>{
  it('fills the shared test account credentials',async()=>{
    render(<MemoryRouter><AuthPage/></MemoryRouter>);
    await userEvent.click(screen.getByRole('button',{name:'Войти как пользователь'}));
    expect(screen.getByLabelText('Логин')).toHaveValue('demo');
    expect(screen.getByLabelText('Пароль')).toHaveValue('demo12345');
  });

  it('fills the administrator test account credentials',async()=>{
    render(<MemoryRouter><AuthPage/></MemoryRouter>);
    await userEvent.click(screen.getByRole('button',{name:'Войти как администратор'}));
    expect(screen.getByLabelText('Логин')).toHaveValue('admin');
    expect(screen.getByLabelText('Пароль')).toHaveValue('admin12345');
  });

  it('shows a loader and prevents repeated input while login is pending',async()=>{
    let finishLogin:(value:unknown)=>void=()=>{};
    const pendingLogin=new Promise(resolve=>{finishLogin=resolve});
    vi.spyOn(authApi,'login').mockReturnValue(pendingLogin);
    const user=userEvent.setup();
    render(<MemoryRouter><AuthPage/></MemoryRouter>);

    await user.type(screen.getByLabelText('Логин'),'demo');
    await user.type(screen.getByLabelText('Пароль'),'demo12345');
    await user.click(screen.getByRole('button',{name:'Войти'}));

    const submitButton=screen.getByRole('button',{name:'Подождите…'});
    expect(submitButton).toBeDisabled();
    expect(submitButton.querySelector('.button-spinner')).toBeInTheDocument();
    expect(submitButton.closest('form')).toHaveAttribute('aria-busy','true');
    expect(screen.getByLabelText('Логин')).toBeDisabled();

    finishLogin({token:'test-token',user:{id:1,username:'demo',displayName:'Demo'}});
  });
});
