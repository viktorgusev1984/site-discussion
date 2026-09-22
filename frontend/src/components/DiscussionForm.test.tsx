import {render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe,it,expect,vi} from 'vitest';

const {improve}=vi.hoisted(()=>({improve:vi.fn()}));
vi.mock('../api/discussions',()=>({aiApi:{improve},uploadAttachment:vi.fn()}));
import DiscussionForm from './DiscussionForm';

const categories=[{id:1,name:'Идеи',slug:'ideas',color:'#0f0'}];

async function failImprove(rejection:unknown){
  improve.mockRejectedValueOnce(rejection);
  render(<DiscussionForm categories={categories} onSubmit={vi.fn()}/>);
  await userEvent.type(screen.getByLabelText('Заголовок'),'Полезная идея');
  await userEvent.type(screen.getByLabelText('Описание'),'Подробное описание идеи');
  await userEvent.click(screen.getByRole('button',{name:/Улучшить с AI/}));
  return screen.findByRole('alert');
}

describe('DiscussionForm',()=>{
  it('validates and submits a new discussion',async()=>{
    const submit=vi.fn();
    render(<DiscussionForm categories={categories} onSubmit={submit}/>);
    await userEvent.click(screen.getByText('Опубликовать идею'));
    expect(screen.getByText('Введите не менее 5 символов')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Заголовок'),'Полезная идея');
    await userEvent.type(screen.getByLabelText('Описание'),'Подробное описание идеи');
    await userEvent.click(screen.getByText('Опубликовать идею'));
    expect(submit).toHaveBeenCalledWith({title:'Полезная идея',body:'Подробное описание идеи',categoryId:1});
  });

  it('shows the reason reported by the backend when an AI draft fails',async()=>{
    const alert=await failImprove({response:{data:{error:'AI-помощник не смог выполнить запрос'}}});
    expect(alert).toHaveTextContent('AI-помощник не смог выполнить запрос');
  });

  it('falls back to a generic message when an AI failure carries no reason',async()=>{
    const alert=await failImprove(new Error('network down'));
    expect(alert).toHaveTextContent('AI-помощник временно недоступен');
  });
});
