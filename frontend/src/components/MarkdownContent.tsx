import ReactMarkdown,{defaultUrlTransform} from 'react-markdown';

const apiBase=import.meta.env.VITE_API_URL||'/api';

/**
 * Attachment URLs are stored in Markdown as `/api/attachments/...`.  When the
 * frontend and API are deployed on different origins, point those URLs at the
 * configured API host instead of the static-site host.
 */
export function resolveMarkdownUrl(url:string,base=apiBase){
 if(!url.startsWith('/api/attachments/'))return defaultUrlTransform(url);
 if(base.startsWith('/'))return defaultUrlTransform(url);
 try{return defaultUrlTransform(new URL(url,new URL(base,window.location.origin).origin).toString())}
 catch{return defaultUrlTransform(url)}
}

export default function MarkdownContent({children}:{children:string}){
 return <ReactMarkdown urlTransform={url=>resolveMarkdownUrl(url)}>{children}</ReactMarkdown>;
}
