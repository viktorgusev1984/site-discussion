import {cleanup,render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach,describe,expect,it,vi} from 'vitest';
vi.mock('../api/discussions',()=>({uploadAttachment:vi.fn(async(file:File)=>({name:file.name,url:`/files/${file.name}`}))}));
afterEach(cleanup);
import MarkdownEditor from './MarkdownEditor';
describe('MarkdownEditor',()=>{
 it('applies GitHub-style markdown formatting to selected text',async()=>{const user=userEvent.setup(),onChange=vi.fn();render(<MarkdownEditor value="текст" onChange={onChange}/>);const editor=screen.getByRole('tabpanel',{name:'Написать'}) as HTMLTextAreaElement;editor.setSelectionRange(0,5);await user.click(screen.getByRole('button',{name:/Жирный/}));expect(onChange).toHaveBeenCalledWith('**текст**')});
 it('switches between writing and preview tabs',async()=>{const user=userEvent.setup();render(<MarkdownEditor value="**готово**" onChange={vi.fn()}/>);await user.click(screen.getByRole('tab',{name:'Предпросмотр'}));expect(screen.getByRole('tabpanel',{name:'Предпросмотр'})).toHaveTextContent('готово');expect(screen.queryByRole('textbox')).not.toBeInTheDocument()});
 it('opens the additional-functions menu',async()=>{const user=userEvent.setup();render(<MarkdownEditor value="" onChange={vi.fn()}/>);await user.click(screen.getByRole('button',{name:'Дополнительные функции'}));expect(screen.getByRole('menu')).toBeInTheDocument();expect(screen.getByRole('menuitem',{name:'Прикрепить файлы'})).toBeInTheDocument()});
 it('uploads documents and inserts a Markdown download link',async()=>{const user=userEvent.setup(),onChange=vi.fn();render(<MarkdownEditor value="Описание" onChange={onChange}/>);const editor=screen.getByRole('tabpanel',{name:'Написать'}) as HTMLTextAreaElement;editor.setSelectionRange(8,8);const input=document.querySelector('input[type="file"]') as HTMLInputElement;await user.upload(input,new File(['report'],'отчёт.pdf',{type:'application/pdf'}));expect(onChange).toHaveBeenCalledWith('Описание\n\n[отчёт.pdf](/files/отчёт.pdf)\n')});
 it('uploads images and inserts an image preview',async()=>{const user=userEvent.setup(),onChange=vi.fn();render(<MarkdownEditor value="" onChange={onChange}/>);const input=document.querySelector('input[type="file"]') as HTMLInputElement;await user.upload(input,new File(['image'],'screen.png',{type:'image/png'}));expect(onChange).toHaveBeenCalledWith('![screen.png](/files/screen.png)\n')});
});
