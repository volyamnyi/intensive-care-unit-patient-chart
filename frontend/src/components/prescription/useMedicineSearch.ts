import { useState, useEffect, useRef, useCallback } from 'react';
import { prescriptionApi } from '../../api/medication';
import type { MedicineCatalogItem } from '../../types/medication';

/** Debounce interval for live medicine-catalog searches (milliseconds). */
export const MEDICINE_SEARCH_DEBOUNCE_MS = 300;

export interface UseMedicineSearchResult {
  /** Catalog items for the last settled search (empty while pending). */
  options: MedicineCatalogItem[];
  /** A catalog request is in flight. */
  loading: boolean;
  /** True once the last request failed. Empty results are NOT an error —
   *  derive them from `options.length === 0 && !loading && !error`. */
  error: boolean;
  /** The search field has crossed the 2-char threshold (dropdown may be visible). */
  active: boolean;
  /** Re-run the last search immediately (used by the error-state retry). */
  retry: () => void;
}

/**
 * Single live medicine-catalog search contract shared by every
 * `onSearchMedicine` entry point: `MedicineSearchInput` (the «Листок» add row)
 * and `PrescriptionItemForm` (item creation).
 *
 * - debounces every keystroke (~300 ms) to avoid hammering MIS on each press;
 * - aborts the in-flight request on the next change via `AbortController`;
 * - ignores stale responses: only the most recent query's result lands, so a
 *   slow `query A` can never overwrite a faster `query B`.
 *
 * No client-side cache and no fallback data — the catalog comes from MIS
 * (`GET /prescriptions/medicine-catalog`) and loading/empty/error are honest.
 */
export function useMedicineSearch(
  keyword: string,
  search?: (query: string, signal?: AbortSignal) => Promise<MedicineCatalogItem[]>,
): UseMedicineSearchResult {
  const searchImpl = useCallback(
    (query: string, signal?: AbortSignal) =>
      search ? search(query, signal) : prescriptionApi.getMedicineCatalog(query, signal).then(r => r.data),
    [search],
  );

  const [options, setOptions] = useState<MedicineCatalogItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  // Manual retry counter — bumping it re-runs the effect even though `keyword`
  // has not changed (retry keeps the last valid query).
  const [attempt, setAttempt] = useState(0);

  const latestRef = useRef(0);
  const controllerRef = useRef<AbortController | null>(null);

  // Settle all state the moment the query identity changes so the UI shows the
  // pending window immediately and never a stale loader/empty/error from the
  // previous query. The actual fetch is debounced to protect MIS.
  useEffect(() => {
    setOptions([]);
    setError(false);
    const isShort = keyword.trim().length < 2;
    setLoading(!isShort);
    if (isShort) return;
    const timer = setTimeout(() => {
      latestRef.current += 1;
      const id = latestRef.current;
      controllerRef.current?.abort();
      const controller = new AbortController();
      controllerRef.current = controller;
      searchImpl(keyword, controller.signal)
        .then((res) => {
          if (id === latestRef.current) {
            const items = Array.isArray(res) ? res : [];
            setOptions(items);
            setLoading(false);
            setError(false);
          }
        })
        .catch(() => {
          if (id === latestRef.current && !controller.signal.aborted) {
            setOptions([]);
            setLoading(false);
            setError(true);
          }
        });
    }, MEDICINE_SEARCH_DEBOUNCE_MS);
    return () => {
      clearTimeout(timer);
      controllerRef.current?.abort();
    };
  }, [keyword, attempt, searchImpl]);

  const retry = useCallback(() => setAttempt((a) => a + 1), []);

  return { options, loading, error, active: keyword.trim().length >= 2, retry };
}
