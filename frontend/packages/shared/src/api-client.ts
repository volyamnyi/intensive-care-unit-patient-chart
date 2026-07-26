import axios from 'axios';

const API_BASE = '/api';

export const createApiClient = (baseURL?: string) => {
  const client = axios.create({
    baseURL: baseURL ?? API_BASE,
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

  return client;
};

export type ApiClient = ReturnType<typeof createApiClient>;
