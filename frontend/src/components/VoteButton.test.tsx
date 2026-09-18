import {render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach,describe,it,expect,vi} from 'vitest';
import VoteButton from './VoteButton';
import {discussionsApi} from '../api/discussions';
import {AuthProvider} from '../auth/AuthContext';

const vote=vi.spyOn(discussionsApi,'vote').mockResolvedValue({});

describe('VoteButton',()=>{
  beforeEach(()=>{localStorage.clear();vote.mockClear()});

  it('toggles the vote optimistically for an authenticated user',async()=>{
    localStorage.setItem('currentUser',JSON.stringify({id:1,username:'demo',displayName:'Demo'}));
    render(<AuthProvider><VoteButton id={1} count={3}/></AuthProvider>);
    const button=screen.getByRole('button');
    await userEvent.click(button);
    expect(button).toHaveAttribute('aria-pressed','true');
    expect(screen.getByText('4')).toBeInTheDocument();
  });

  it('is read-only for a guest',async()=>{
    render(<AuthProvider><VoteButton id={1} count={3}/></AuthProvider>);
    const button=screen.getByRole('button',{name:'Войдите, чтобы проголосовать'});
    expect(button).toBeDisabled();
    await userEvent.click(button);
    expect(vote).not.toHaveBeenCalled();
    expect(screen.getByText('3')).toBeInTheDocument();
  });
});
