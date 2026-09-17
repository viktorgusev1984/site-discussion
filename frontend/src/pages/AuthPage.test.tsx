import {render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {describe,expect,it} from 'vitest';
import AuthPage from './AuthPage';

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
});
