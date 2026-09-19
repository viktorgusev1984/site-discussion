import {render,screen,waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach,describe,expect,it,vi} from 'vitest';
import NotificationSettingsPage from './NotificationSettingsPage';
import {notificationsApi} from '../api/notifications';

vi.mock('../api/notifications',()=>({notificationsApi:{
  channels:vi.fn(),rules:vi.fn(),createChannel:vi.fn(),updateChannel:vi.fn(),
  disableChannel:vi.fn(),deleteChannel:vi.fn(),testChannel:vi.fn(),
  createRule:vi.fn(),updateRule:vi.fn(),deleteRule:vi.fn()
}}));

const channel={id:7,type:'WEBHOOK' as const,name:'Deploy hook',active:true,
  connection:'hooks.example/…cdef',createdAt:'2026-09-19T00:00:00Z',lastSuccessfulCheckAt:null};

describe('notification settings',()=>{
  beforeEach(()=>{
    vi.clearAllMocks();
    vi.mocked(notificationsApi.channels).mockResolvedValue([channel]);
    vi.mocked(notificationsApi.rules).mockResolvedValue([]);
  });

  it('loads connections without exposing a saved secret',async()=>{
    render(<NotificationSettingsPage/>);
    expect(screen.getByText('Загрузка настроек…')).toBeInTheDocument();
    expect(await screen.findByRole('heading',{name:'Deploy hook'})).toBeInTheDocument();
    expect(screen.getByText('hooks.example/…cdef')).toBeInTheDocument();
    expect(screen.queryByDisplayValue(/secret/i)).not.toBeInTheDocument();
  });

  it('creates a signed webhook and clears the secret field',async()=>{
    vi.mocked(notificationsApi.createChannel).mockResolvedValue(channel);
    const user=userEvent.setup();render(<NotificationSettingsPage/>);
    await screen.findByRole('heading',{name:'Deploy hook'});
    await user.selectOptions(screen.getByLabelText('Тип'),'WEBHOOK');
    await user.type(screen.getByLabelText('Пользовательское название'),'CI');
    await user.type(screen.getByLabelText('HTTPS URL'),'https://hooks.example/abc');
    const secret=screen.getByLabelText(/Секрет для подписи/);
    expect(secret).toHaveAttribute('type','password');
    await user.type(secret,'top-secret');
    await user.click(screen.getByRole('button',{name:'Добавить подключение'}));
    await waitFor(()=>expect(notificationsApi.createChannel).toHaveBeenCalledWith({
      name:'CI',type:'WEBHOOK',url:'https://hooks.example/abc',secret:'top-secret'}));
    await waitFor(()=>expect(screen.queryByLabelText(/Секрет для подписи/)).not.toBeInTheDocument());
  });

  it('tests a connection and enables a rule',async()=>{
    vi.mocked(notificationsApi.testChannel).mockResolvedValue({} as never);
    vi.mocked(notificationsApi.createRule).mockResolvedValue({id:8,trigger:'NEW_COMMENT',scope:'ALL_DISCUSSIONS',discussionId:null,channelId:7,active:true});
    const user=userEvent.setup();render(<NotificationSettingsPage/>);
    await user.click(await screen.findByRole('button',{name:'Проверить подключение'}));
    await waitFor(()=>expect(notificationsApi.testChannel).toHaveBeenCalledWith(7));
    await user.selectOptions(screen.getByLabelText('Новый комментарий — Deploy hook'),'on');
    await waitFor(()=>expect(notificationsApi.createRule).toHaveBeenCalledWith({trigger:'NEW_COMMENT',channelId:7,scope:'ALL_DISCUSSIONS',discussionId:null,active:true}));
  });

  it('shows an API error',async()=>{
    vi.mocked(notificationsApi.channels).mockRejectedValue({isAxiosError:true,response:{data:{message:'Доступ запрещён'}}});
    render(<NotificationSettingsPage/>);
    expect(await screen.findByRole('alert')).toHaveTextContent('Доступ запрещён');
  });
});
