import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { printPdfUrl, printPdfBlob } from '../../lib/printPdf';

describe('printPdf', () => {
  // jsdom attempts a real subresource load for iframe src URLs and raises an
  // opaque-origin SecurityError asynchronously (attributed to a random test),
  // so every iframe created here gets a load-neutralized src setter: the util
  // still assigns/reads the value, but no navigation ever happens. Handlers
  // are driven manually via the recorded element.
  let lastFrame: HTMLIFrameElement | null = null;
  let lastSrc = '';

  beforeEach(() => {
    vi.useFakeTimers();
    window.URL.createObjectURL = vi.fn(() => 'blob:mock-pdf');
    window.URL.revokeObjectURL = vi.fn();
    lastFrame = null;
    lastSrc = '';
    const realCreate = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation(((
      tagName: string,
    ) => {
      const el = realCreate(tagName);
      if (tagName === 'iframe') {
        Object.defineProperty(el, 'src', {
          configurable: true,
          get: () => lastSrc,
          set: (v: string) => {
            lastSrc = v;
          },
        });
        lastFrame = el as HTMLIFrameElement;
      }
      return el;
    }) as unknown as typeof document.createElement);
  });

  afterEach(() => {
    document.querySelectorAll('iframe[title="PDF print frame"]').forEach((f) => f.remove());
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('printPdfUrl appends a hidden iframe with the pdf url', async () => {
    const pending = printPdfUrl('blob:mock-pdf');
    const frame = lastFrame;
    expect(frame).not.toBeNull();
    expect(frame?.style.display).toBe('none');
    expect(lastSrc).toBe('blob:mock-pdf');
    expect(frame && document.body.contains(frame)).toBe(true);
    frame?.dispatchEvent(new Event('load'));
    await pending;
    expect(frame && document.body.contains(frame)).toBe(false);
  });

  it('printPdfUrl rejects when the iframe errors', async () => {
    const pending = printPdfUrl('blob:mock-pdf');
    const frame = lastFrame;
    expect(frame).not.toBeNull();
    frame?.dispatchEvent(new Event('error'));
    await expect(pending).rejects.toThrow('failed to load');
    expect(frame && document.body.contains(frame)).toBe(false);
  });

  it('printPdfBlob creates an object URL, prints, then revokes it', async () => {
    const blob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
    const pending = printPdfBlob(blob);
    expect(window.URL.createObjectURL).toHaveBeenCalledWith(blob);
    expect(lastSrc).toBe('blob:mock-pdf');
    lastFrame?.dispatchEvent(new Event('load'));
    await pending;
    expect(window.URL.revokeObjectURL).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(1000);
    expect(window.URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-pdf');
  });
});
