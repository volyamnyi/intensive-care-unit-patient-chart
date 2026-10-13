import type { ReactNode } from 'react';

import { cn } from '@/lib/utils';

export interface PageContainerProps {
  children: ReactNode;
  /**
   * Shell width:
   * - `narrow` — `max-w-page-narrow` (42rem) — маленькі форми/списки (наприклад, review dialog);
   * - `page`   — `max-w-page` (80rem ≈ 1280px) — **дефолт для всіх робочих сторінок** (еквівалент старого `max-w-7xl`);
   * - `wide`   — `max-w-page-wide` (120rem ≈ 1920px) — только логічні зони забезпечення вмісту наясні;
   * - `full`   — без обмеження (`w-full`) — `/prosthetics/process/*` має власні gutter-и (`ProcessLayout`).
   */
  width?: 'narrow' | 'page' | 'wide' | 'full';
  className?: string;
}

export function PageContainer({ children, width = 'page', className }: PageContainerProps) {
  const widthClass = {
    narrow: 'max-w-page-narrow',
    page: 'max-w-page',
    wide: 'max-w-page-wide',
    full: 'w-full max-w-none',
  }[width];

  return (
    <div className={cn('mx-auto w-full px-4 sm:px-6', widthClass, className)}>
      {children}
    </div>
  );
}
