import {useRef, useState} from 'react';
import ReactMarkdown from 'react-markdown';
import {uploadImage} from '../api/discussions';

type Props = {value:string;onChange:(value:string)=>void;error?:string;label?:string;placeholder?:string};

export default function MarkdownEditor({value,onChange,error,label='Описание',placeholder='Подробно опишите идею…'}:Props){
  const [preview,setPreview]=useState(false),[uploading,setUploading]=useState(false),[uploadError,setUploadError]=useState('');
  const textarea=useRef<HTMLTextAreaElement>(null),fileInput=useRef<HTMLInputElement>(null);
  function insert(before:string,after='',fallback=''){
    const field=textarea.current,start=field?.selectionStart??value.length,end=field?.selectionEnd??value.length,selected=value.slice(start,end)||fallback;
    onChange(`${value.slice(0,start)}${before}${selected}${after}${value.slice(end)}`);
    requestAnimationFrame(()=>{field?.focus();field?.setSelectionRange(start+before.length,start+before.length+selected.length)});
  }
  async function attach(files:FileList|File[]){
    const images=Array.from(files).filter(file=>file.type.startsWith('image/'));
    if(!images.length){setUploadError('Можно прикреплять только изображения');return}
    setUploading(true);setUploadError('');
    try{
      const uploaded=await Promise.all(images.map(uploadImage));
      const markdown=uploaded.map(item=>`![${item.name}](${item.url})`).join('\n\n');
      insert(`${value&&!value.endsWith('\n')?'\n\n':''}${markdown}\n`);
    }catch{setUploadError('Не удалось загрузить изображение. Попробуйте ещё раз.')}
    finally{setUploading(false);if(fileInput.current)fileInput.current.value=''}
  }
  return <div className={`editor ${error?'invalid':''}`}>
    <div className="editor-tabs"><button type="button" className={!preview?'active':''} onClick={()=>setPreview(false)}>Написать</button><button type="button" className={preview?'active':''} onClick={()=>setPreview(true)}>Предпросмотр</button><span>Поддерживается Markdown</span></div>
    {!preview&&<div className="editor-toolbar" aria-label="Панель форматирования">
      <button type="button" title="Жирный" aria-label="Жирный" onClick={()=>insert('**','**','текст')}><b>B</b></button>
      <button type="button" title="Курсив" aria-label="Курсив" onClick={()=>insert('_','_','текст')}><i>I</i></button>
      <button type="button" title="Заголовок" aria-label="Формат: заголовок" onClick={()=>insert('## ','','Заголовок')}>H</button>
      <button type="button" title="Ссылка" aria-label="Ссылка" onClick={()=>insert('[','](https://)','текст ссылки')}>🔗</button>
      <button type="button" title="Список" aria-label="Список" onClick={()=>insert('- ','','пункт списка')}>☷</button>
      <button type="button" className="attach-button" disabled={uploading} onClick={()=>fileInput.current?.click()}>📎 {uploading?'Загрузка…':'Прикрепить изображение'}</button>
      <input ref={fileInput} className="visually-hidden" type="file" accept="image/png,image/jpeg,image/gif,image/webp" multiple onChange={event=>event.target.files&&attach(event.target.files)}/>
    </div>}
    {preview?<div className="preview markdown"><ReactMarkdown>{value||'*Здесь появится предпросмотр*'}</ReactMarkdown></div>:<textarea ref={textarea} aria-label={label} value={value} onChange={event=>onChange(event.target.value)} onPaste={event=>{const images=Array.from(event.clipboardData.files).filter(file=>file.type.startsWith('image/'));if(images.length){event.preventDefault();attach(images)}}} onDragOver={event=>event.preventDefault()} onDrop={event=>{if(event.dataTransfer.files.length){event.preventDefault();attach(event.dataTransfer.files)}}} placeholder={placeholder} rows={12}/>}
    <div className="editor-help">Перетащите картинку сюда или вставьте скриншот из буфера обмена</div>
    {(error||uploadError)&&<small className="error">{error||uploadError}</small>}
  </div>
}
