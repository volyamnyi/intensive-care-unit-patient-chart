/**
 * Print a PDF blob in-module (Phase 16, #269): MIS transfer is banned, so
 * generated PDFs are downloaded/printed locally instead of sent to MIS.
 *
 * The blob opens in a hidden same-document iframe and prints once loaded;
 * the iframe is removed afterwards and a caller-owned object URL is revoked
 * on the next tick (the print job already captured the document).
 */
export function printPdfUrl(url: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (typeof document === 'undefined') {
      reject(new Error('printPdfUrl needs a DOM document'));
      return;
    }
    const frame = document.createElement('iframe');
    frame.style.display = 'none';
    frame.setAttribute('title', 'PDF print frame');
    frame.src = url;
    const done = (ok: boolean) => {
      frame.remove();
      if (ok) resolve();
      else reject(new Error('PDF iframe failed to load for printing'));
    };
    frame.onload = () => {
      try {
        const win = frame.contentWindow;
        if (win && typeof win.print === 'function') win.print();
      } catch {
        // Print dispatch itself must never reject the flow (headless viewers).
      }
      done(true);
    };
    frame.onerror = () => done(false);
    document.body.appendChild(frame);
  });
}

export async function printPdfBlob(blob: Blob): Promise<void> {
  const url = window.URL.createObjectURL(blob);
  try {
    await printPdfUrl(url);
  } finally {
    window.setTimeout(() => window.URL.revokeObjectURL(url), 1000);
  }
}
