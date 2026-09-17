import {cleanup,render,screen} from '@testing-library/react';
import {afterEach,describe,expect,it} from 'vitest';
import MarkdownContent,{resolveMarkdownUrl} from './MarkdownContent';

afterEach(cleanup);

describe('MarkdownContent',()=>{
 it('resolves attachment URLs against an API deployed on another origin',()=>{
  expect(resolveMarkdownUrl('/api/attachments/file-id','https://api.example.com/api')).toBe('https://api.example.com/api/attachments/file-id');
 });

 it('keeps attachment URLs relative when the API shares the frontend origin',()=>{
  expect(resolveMarkdownUrl('/api/attachments/file-id','/api')).toBe('/api/attachments/file-id');
 });

 it('uses the resolved URL for image previews',()=>{
  render(<MarkdownContent>{'![photo](/api/attachments/file-id)'}</MarkdownContent>);
  expect(screen.getByRole('img',{name:'photo'})).toHaveAttribute('src',resolveMarkdownUrl('/api/attachments/file-id'));
 });

 it('retains the markdown renderer protocol protection',()=>{
  expect(resolveMarkdownUrl('javascript:alert(1)','https://api.example.com/api')).toBe('');
 });
});
