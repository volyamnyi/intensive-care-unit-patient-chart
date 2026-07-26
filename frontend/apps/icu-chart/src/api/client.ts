import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api';

const client = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
    }
    return Promise.reject(err);
  }
);

export default client;
