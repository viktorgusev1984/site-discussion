import {cleanup,render,screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {afterEach,describe,expect,it,vi} from 'vitest';
afterEach(cleanup);
import MarkdownEditor from './MarkdownEditor';
describe('MarkdownEditor',()=>{
 it('applies GitHub-style markdown formatting to selected text',async()=>{const user=userEvent.setup(),onChange=vi.fn();render(<MarkdownEditor value="текст" onChange={onChange}/>);const editor=screen.getByRole('tabpanel',{name:'Написать'}) as HTMLTextAreaElement;editor.setSelectionRange(0,5);await user.click(screen.getByRole('button',{name:/Жирный/}));expect(onChange).toHaveBeenCalledWith('**текст**')});
 it('switches between writing and preview tabs',async()=>{const user=userEvent.setup();render(<MarkdownEditor value="**готово**" onChange={vi.fn()}/>);await user.click(screen.getByRole('tab',{name:'Предпросмотр'}));expect(screen.getByRole('tabpanel',{name:'Предпросмотр'})).toHaveTextContent('готово');expect(screen.queryByRole('textbox')).not.toBeInTheDocument()});
});
