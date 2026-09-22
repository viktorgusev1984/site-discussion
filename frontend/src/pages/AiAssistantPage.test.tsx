import {render,screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach,describe,expect,it,vi} from 'vitest';

const {chatSession}=vi.hoisted(()=>({chatSession:vi.fn()}));
vi.mock('../api/discussions',()=>({aiApi:{chatSession}}));
vi.mock('@qwen-code/web-shell',()=>({WebShellWithProviders:({sessionId}:{sessionId:string})=><div data-testid="qwen-web-shell">{sessionId}</div>}));
import AiAssistantPage from './AiAssistantPage';

describe('AI assistant conversation',()=>{
  beforeEach(()=>{localStorage.setItem('token','application-jwt');chatSession.mockResolvedValue({sessionId:'550e8400-e29b-41d4-a716-446655440000'})});
  it('opens the user session in the Qwen web template',async()=>{
    render(<MemoryRouter><AiAssistantPage/></MemoryRouter>);
    expect(screen.getByRole('status')).toHaveTextContent('Подключаем');
    expect(await screen.findByTestId('qwen-web-shell')).toHaveTextContent('550e8400-e29b-41d4-a716-446655440000');
    expect(screen.getByRole('heading',{name:'AI-помощник'})).toBeInTheDocument();
  });
  it('surfaces the reason reported by the backend when the session cannot be opened',async()=>{
    chatSession.mockRejectedValueOnce({response:{data:{error:'AI-помощник не настроен'}}});
    render(<MemoryRouter><AiAssistantPage/></MemoryRouter>);
    expect(await screen.findByRole('alert')).toHaveTextContent('AI-помощник не настроен');
  });
});
