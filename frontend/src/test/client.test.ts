import { describe, it, expect, beforeEach, vi } from 'vitest';
import client from '../api/client';

beforeEach(() => {
  localStorage.clear();
  vi.restoreAllMocks();
});

describe('API client', () => {
  it('has baseURL set to localhost:8085/api', () => {
    expect(client.defaults.baseURL).toBe('http://localhost:8085/api');
  });

  it('attaches Bearer token from localStorage on requests', async () => {
    localStorage.setItem('token', 'test-jwt');
    const requestInterceptor = client.interceptors.request as any;
    const handler = requestInterceptor.handlers[0].fulfilled;
    const config = { headers: {} };
    const result = await handler(config);
    expect(result.headers.Authorization).toBe('Bearer test-jwt');
  });

  it('does not attach token when localStorage is empty', async () => {
    const requestInterceptor = client.interceptors.request as any;
    const handler = requestInterceptor.handlers[0].fulfilled;
    const config = { headers: {} };
    const result = await handler(config);
    expect(result.headers.Authorization).toBeUndefined();
  });

  it('clears token and redirects on 401', async () => {
    localStorage.setItem('token', 'test-jwt');
    delete (window as any).location;
    window.location = { href: '' } as any;

    const responseInterceptor = client.interceptors.response as any;
    const errorHandler = responseInterceptor.handlers[0].rejected;
    const error = { response: { status: 401 } };

    await expect(errorHandler(error)).rejects.toEqual(error);
    expect(localStorage.getItem('token')).toBeNull();
    expect(window.location.href).toBe('/login');
  });

  it('does not redirect on non-401 errors', async () => {
    delete (window as any).location;
    window.location = { href: '' } as any;

    const responseInterceptor = client.interceptors.response as any;
    const errorHandler = responseInterceptor.handlers[0].rejected;
    const error = { response: { status: 500 } };

    await expect(errorHandler(error)).rejects.toEqual(error);
    expect(window.location.href).toBe('');
  });
});
