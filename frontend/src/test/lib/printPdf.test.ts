import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { printPdfUrl, printPdfBlob } from '../../lib/printPdf';

describe('printPdf', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    window.URL.createObjectURL = vi.fn(() => 'blob:mock-pdf');
    window.URL.revokeObjectURL = vi.fn();
  });

  afterEach(() => {
    document.querySelectorAll('iframe[title="PDF print frame"]').forEach((f) => f.remove());
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('printPdfUrl appends a hidden iframe with the pdf url', async () => {
    const pending = printPdfUrl('blob:mock-pdf');
    const frame = document.querySelector<HTMLIFrameElement>('iframe[title="PDF print frame"]');
    expect(frame).not.toBeNull();
    expect(frame!.style.display).toBe('none');
    expect(frame!.getAttribute('src')).toBe('blob:mock-pdf');
    frame!.dispatchEvent(new Event('load'));
    await pending;
    expect(document.querySelector('iframe[title="PDF print frame"]')).toBeNull();
  });

  it('printPdfUrl rejects when the iframe errors', async () => {
    const pending = printPdfUrl('blob:mock-pdf');
    const frame = document.querySelector<HTMLIFrameElement>('iframe[title="PDF print frame"]');
    frame!.dispatchEvent(new Event('error'));
    await expect(pending).rejects.toThrow('failed to load');
  });

  it('printPdfBlob creates an object URL, prints, then revokes it', async () => {
    const blob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
    const pending = printPdfBlob(blob);
    expect(window.URL.createObjectURL).toHaveBeenCalledWith(blob);
    const frame = document.querySelector<HTMLIFrameElement>('iframe[title="PDF print frame"]');
    frame!.dispatchEvent(new Event('load'));
    await pending;
    expect(window.URL.revokeObjectURL).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(1000);
    expect(window.URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-pdf');
  });
});
