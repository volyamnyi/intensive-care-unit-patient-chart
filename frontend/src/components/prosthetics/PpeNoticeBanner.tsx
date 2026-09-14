import { Hand, TriangleAlert } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import type { PpeNotice } from '@/prosthetics/ppeNotices';

/**
 * Інформаційний (non-blocking) банер засобів індивідуального захисту
 * у візарді «Виробництво протезів» (епік PPE, #294 full-kit, #295 nitrile).
 *
 * Чистий презентаційний компонент: жодних інтерактивних елементів
 * (інваріант для `wizard-checkbox-surface.spec.ts` — банер не додає
 * чекбоксів/клікабельних зон), крок завершується звичайною кнопкою
 * «Готово →», валідація `completeStep` не зачеплена.
 *
 * Візуальне розрізнення: `full-kit` — бурштиновий `warning`-Alert з сіткою
 * 4 фото; `nitrile` — спокійний `default`-Alert з одним фото. Невідомий
 * kind рендериться в `null`, а не падає.
 */
export default function PpeNoticeBanner({ notice }: { notice: PpeNotice }) {
  if (notice.kind === 'nitrile') {
    const [img] = notice.images;
    return (
      <Alert data-testid={`ppe-notice-${notice.kind}`}>
        <Hand className="size-4" aria-hidden="true" />
        <AlertTitle>{notice.title}</AlertTitle>
        <AlertDescription>
          <div className="grid grid-cols-1 items-center gap-3 md:grid-cols-[auto_1fr]">
            {img && (
              <figure
                key={img.src}
                className="flex flex-col items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-3"
              >
                <img
                  src={img.src}
                  alt={img.alt}
                  loading="lazy"
                  className="h-40 w-auto rounded-lg object-contain md:h-48"
                />
                <figcaption className="mt-2 text-center text-xs text-muted-foreground">
                  {img.caption}
                </figcaption>
              </figure>
            )}
            <p className="font-medium text-card-foreground">{notice.text}</p>
          </div>
        </AlertDescription>
      </Alert>
    );
  }
  if (notice.kind !== 'full-kit') return null;
  return (
    <Alert variant="warning" data-testid={`ppe-notice-${notice.kind}`}>
      <TriangleAlert className="size-4" aria-hidden="true" />
      <AlertTitle>{notice.title}</AlertTitle>
      <AlertDescription>
        <p className="font-medium text-card-foreground">{notice.text}</p>
        <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {notice.images.map((img) => (
            <figure
              key={img.src}
              className="flex flex-col items-center rounded-xl border-2 border-primary/20 bg-primary/5 p-3"
            >
              <img
                src={img.src}
                alt={img.alt}
                loading="lazy"
                className="h-40 w-auto rounded-lg object-contain"
              />
              <figcaption className="mt-2 text-center text-xs text-muted-foreground">
                {img.caption}
              </figcaption>
            </figure>
          ))}
        </div>
      </AlertDescription>
    </Alert>
  );
}
