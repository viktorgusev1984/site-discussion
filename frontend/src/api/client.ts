import axios from 'axios';
const api=axios.create({baseURL:import.meta.env.VITE_API_URL || '/api',headers:{'Content-Type':'application/json'}});
api.interceptors.request.use(config=>{const token=localStorage.getItem('token');if(token) config.headers.Authorization=`Bearer ${token}`;return config});
api.interceptors.response.use(response=>response,error=>{if((error as {response?:{status?:number}})?.response?.status===401)window.dispatchEvent(new Event('auth:expired'));return Promise.reject(error)});
export function apiError(e:unknown,fallback:string){const message=(e as {response?:{data?:{error?:unknown}}})?.response?.data?.error;return typeof message==='string'&&message.trim()?message:fallback}
export default api;
