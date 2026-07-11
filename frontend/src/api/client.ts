import axios from 'axios';

const API_BASE = 'http://localhost:8085/api';

const client = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      // Only force a redirect to /login when an authenticated session has
      // expired. During the login attempt itself there is no token yet, so a
      // 401 means "invalid credentials" and must be surfaced to the user
      // (the login form shows an error) rather than triggering a reload.
      const token = localStorage.getItem('token');
      if (token) {
        localStorage.removeItem('token');
        window.location.href = '/login';
      }
    }
    return Promise.reject(err);
  }
);

export default client;
