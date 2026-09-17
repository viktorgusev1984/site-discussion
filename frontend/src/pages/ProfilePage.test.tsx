import {render,screen} from '@testing-library/react';
import {MemoryRouter,Route,Routes} from 'react-router-dom';
import {describe,expect,it,vi} from 'vitest';
import {authApi} from '../api/discussions';
import ProfilePage from './ProfilePage';

describe('profile page',()=>{
  it('shows the requested user and an empty ideas state',async()=>{
    const profile=vi.spyOn(authApi,'profile').mockResolvedValueOnce({
      id:7,username:'anna',displayName:'Анна Смирнова',
      bio:'Развиваю продукт вместе с командой',role:'USER',
      createdAt:'2025-02-12T10:00:00Z',discussions:[]
    });
    render(<MemoryRouter initialEntries={['/users/anna']}>
      <Routes><Route path="/users/:username" element={<ProfilePage/>}/></Routes>
    </MemoryRouter>);
    expect(await screen.findByRole('heading',{name:'Анна Смирнова'})).toBeInTheDocument();
    expect(profile).toHaveBeenCalledWith('anna');
    expect(screen.getByText('@anna')).toBeInTheDocument();
    expect(screen.getByText('Развиваю продукт вместе с командой')).toBeInTheDocument();
    expect(screen.getByText('Пользователь пока не опубликовал ни одной идеи')).toBeInTheDocument();
  });

  it('shows an accessible error when loading fails',async()=>{
    vi.spyOn(authApi,'profile').mockRejectedValueOnce(new Error('network error'));
    render(<MemoryRouter initialEntries={['/users/missing']}>
      <Routes><Route path="/users/:username" element={<ProfilePage/>}/></Routes>
    </MemoryRouter>);
    expect(await screen.findByRole('alert')).toHaveTextContent('Не удалось загрузить профиль пользователя');
  });
});
