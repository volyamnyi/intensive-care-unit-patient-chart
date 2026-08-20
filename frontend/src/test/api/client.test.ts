import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { AxiosError } from 'axios';

describe('API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.unmock('../../api/client');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('exports a default axios instance', async () => {
    // Using dynamic import to get the real module
    const mod = await import('../../api/client');
    expect(mod.default).toBeDefined();
    expect(typeof mod.default.get).toBe('function');
    expect(typeof mod.default.post).toBe('function');
  });

  it('has interceptors for response handling', async () => {
    const mod = await import('../../api/client');
    expect(mod.default.interceptors).toBeDefined();
    expect(mod.default.interceptors.response).toBeDefined();
  });

  it('dispatches auth:unauthorized custom event on 401', async () => {
    const mod = await import('../../api/client');
    // The registered response-interceptor rejection handler is directly invocable
    const rejected = mod.default.interceptors.response.handlers?.[0]?.rejected;
    expect(rejected).toBeDefined();
    if (!rejected) return;

    const dispatchSpy = vi.spyOn(window, 'dispatchEvent');
    const err401 = new AxiosError(
      'Request failed with status code 401',
      'ERR_BAD_REQUEST',
      undefined,
      undefined,
      { data: {}, status: 401, statusText: 'Unauthorized', headers: {}, config: {} } as never
    );

    // the interceptor must re-reject with the original error (pass-through)
    await expect(rejected(err401)).rejects.toBe(err401);
    expect(dispatchSpy).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'auth:unauthorized' })
    );
  });

  it('does not dispatch auth:unauthorized for non-401 errors', async () => {
    const mod = await import('../../api/client');
    const rejected = mod.default.interceptors.response.handlers?.[0]?.rejected;
    expect(rejected).toBeDefined();
    if (!rejected) return;

    const dispatchSpy = vi.spyOn(window, 'dispatchEvent');
    const err400 = new AxiosError(
      'Request failed with status code 400',
      'ERR_BAD_REQUEST',
      undefined,
      undefined,
      { data: {}, status: 400, statusText: 'Bad Request', headers: {}, config: {} } as never
    );

    await expect(rejected(err400)).rejects.toBe(err400);
    expect(dispatchSpy).not.toHaveBeenCalled();
  });
});