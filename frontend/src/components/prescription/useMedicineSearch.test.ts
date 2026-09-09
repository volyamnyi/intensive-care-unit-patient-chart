import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { act } from 'react';
import { renderHook } from '@testing-library/react';
import { useMedicineSearch, MEDICINE_SEARCH_DEBOUNCE_MS } from './useMedicineSearch';
import type { MedicineCatalogItem } from '../../types/medication';

vi.mock('../../api/medication', () => ({
  prescriptionApi: {
    getMedicineCatalog: vi.fn(() => Promise.resolve([])),
  },
}));

const item = (id: MedicineCatalogItem['id'], name: MedicineCatalogItem['name'], extra?: Partial<MedicineCatalogItem>): MedicineCatalogItem => ({
  id,
  name,
  categoryRef: null,
  ptgCode: null,
  isHighRisk: null,
  ...extra,
});

type SearchFn = (q: string, signal?: AbortSignal) => Promise<MedicineCatalogItem[]>;

function renderSearch(keyword: string, search?: SearchFn) {
  return renderHook(
    ({ q, s }: { q: string; s?: SearchFn }) => useMedicineSearch(q, s),
    { initialProps: { q: keyword, s: search } },
  );
}

async function flushDebounce() {
  await act(async () => {
    vi.advanceTimersByTime(MEDICINE_SEARCH_DEBOUNCE_MS);
  });
  await act(async () => {
    await Promise.resolve();
  });
}

describe('useMedicineSearch', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('debounces: does not call search before the interval elapses', async () => {
    const search = vi.fn(() => Promise.resolve([item(1, 'Penicillin')]));
    const { rerender } = renderSearch('', search);
    rerender({ q: 'Pe', s: search });
    expect(search).not.toHaveBeenCalled();
    await act(async () => {
      vi.advanceTimersByTime(MEDICINE_SEARCH_DEBOUNCE_MS - 1);
    });
    expect(search).not.toHaveBeenCalled();
    await flushDebounce();
    expect(search).toHaveBeenCalledTimes(1);
    expect(search).toHaveBeenCalledWith('Pe', expect.any(AbortSignal));
  });

  it('resets state the moment a new query lands and only the newest result is applied', async () => {
    const search = vi.fn((_q: string, _s?: AbortSignal) => new Promise<MedicineCatalogItem[]>(() => {}));
    const { result, rerender } = renderSearch('', search);
    rerender({ q: 'Pen', s: search });
    expect(result.current.loading).toBe(true);
    expect(result.current.options).toEqual([]);
    expect(result.current.error).toBe(false);
    await act(async () => {
      vi.advanceTimersByTime(MEDICINE_SEARCH_DEBOUNCE_MS);
    });
    expect(search).toHaveBeenCalledTimes(1);
    await flushDebounce();
    expect(result.current.loading).toBe(true);
    expect(result.current.options).toEqual([]);
    expect(result.current.error).toBe(false);
  });

  it('aborts the previous request and ignores its stale result when the query changes', async () => {
    const search = vi.fn((_q: string, signal?: AbortSignal) => {
      expect(signal).toBeInstanceOf(AbortSignal);
      expect(signal).not.toBeNull();
      return new Promise<MedicineCatalogItem[]>(() => {});
    });
    const { result, rerender } = renderSearch('', search);
    rerender({ q: 'Pen', s: search });
    await act(async () => {
      vi.advanceTimersByTime(MEDICINE_SEARCH_DEBOUNCE_MS);
    });
    const signal = search.mock.calls[0][1] as AbortSignal;
    expect(signal).toBeInstanceOf(AbortSignal);
    expect(signal.aborted).toBe(false);
    rerender({ q: 'Penc', s: search });
    expect(signal.aborted).toBe(true);
    await act(async () => {
      vi.advanceTimersByTime(MEDICINE_SEARCH_DEBOUNCE_MS);
    });
    expect(search).toHaveBeenCalledTimes(2);
    await flushDebounce();
    expect(result.current.loading).toBe(true);
    expect(result.current.options).toEqual([]);
  });

  it('shows the result once the request resolves', async () => {
    const cat: MedicineCatalogItem[] = [item(1, 'Penicillin'), item(2, 'Pentazocine')];
    const search = vi.fn(() => Promise.resolve(cat));
    const { result } = renderSearch('Pen', search);
    await flushDebounce();
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBe(false);
    expect(result.current.options).toEqual(cat);
  });

  it('treats empty results as an honest empty state, not an error', async () => {
    const search = vi.fn(() => Promise.resolve([]));
    const { result } = renderSearch('Zzz', search);
    await flushDebounce();
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBe(false);
    expect(result.current.options).toEqual([]);
  });

  it('surfaces a rejection as an error without crashing', async () => {
    const search = vi.fn(() => Promise.reject(new Error('network')));
    const { result } = renderSearch('Pen', search);
    await flushDebounce();
    expect(result.current.loading).toBe(false);
    expect(result.current.error).toBe(true);
    expect(result.current.options).toEqual([]);
  });

  it('retry() re-runs the last successful query', async () => {
    const cat: MedicineCatalogItem[] = [item(1, 'Penicillin')];
    const search = vi.fn(() => Promise.resolve(cat));
    const { result } = renderSearch('Pen', search);
    await flushDebounce();
    expect(search).toHaveBeenCalledTimes(1);
    act(() => {
      result.current.retry();
    });
    expect(search).toHaveBeenCalledTimes(1);
    await flushDebounce();
    expect(search).toHaveBeenCalledTimes(2);
    expect(search).toHaveBeenLastCalledWith('Pen', expect.any(AbortSignal));
    expect(result.current.options).toEqual(cat);
    expect(result.current.error).toBe(false);
  });

  it('falls back to the live catalog API when no custom search is provided', async () => {
    const { prescriptionApi } = await import('../../api/medication');
    const api = prescriptionApi.getMedicineCatalog as unknown as ReturnType<typeof vi.fn>;
    const cat: MedicineCatalogItem[] = [item(1, 'Ceftriaxone')];
    api.mockResolvedValueOnce({ data: cat });
    const { result } = renderSearch('Cef', undefined);
    await flushDebounce();
    expect(api).toHaveBeenCalledWith('Cef', expect.any(AbortSignal));
    expect(result.current.options).toEqual(cat);
    expect(result.current.error).toBe(false);
  });
});
