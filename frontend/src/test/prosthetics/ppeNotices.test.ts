import { describe, it, expect } from 'vitest';
import {
  getPpeNotices,
  PPE_FULL_KIT_TEXT,
  PPE_FULL_KIT_TITLE,
  PPE_NITRILE_TEXT,
  PPE_NITRILE_TITLE,
  PPE_NOTICES_BY_STEP,
} from '@/prosthetics/ppeNotices';

// TP-LL-02 stepId — див. `data-prosth.sql` (шаблон `c0000003`).
const STEP_TRAINING_SOCKET = 'e0000024-0000-0000-0000-000000000024';
const STEP_SOFT_LINER = 'e0000029-0000-0000-0000-000000000029';
const STEP_PLASTER_POSITIVE = 'e0000022-0000-0000-0000-000000000022';
const STEP_TRAINING_FITTING = 'e0000026-0000-0000-0000-000000000026';
const STEP_TRAINING_CORRECTION = 'e0000028-0000-0000-0000-000000000028';
const STEP_PERMANENT_CORRECTION = 'e0000032-0000-0000-0000-000000000032';

describe('ppeNotices', () => {
  it('мапить рівно 6 stepId / 7 повідомлень', () => {
    expect(Object.keys(PPE_NOTICES_BY_STEP)).toHaveLength(6);
    expect(Object.values(PPE_NOTICES_BY_STEP).flat()).toHaveLength(7);
  });

  it('e0000024 (тренувальна гільза) несе обидва нотиси у порядку [full-kit, nitrile]', () => {
    const notices = getPpeNotices(STEP_TRAINING_SOCKET);
    expect(notices.map((n) => n.kind)).toEqual(['full-kit', 'nitrile']);
    expect(notices[0]?.title).toBe(PPE_FULL_KIT_TITLE);
    expect(notices[0]?.text).toBe(PPE_FULL_KIT_TEXT);
    expect(notices[0]?.images).toHaveLength(4);
    expect(notices[1]?.title).toBe(PPE_NITRILE_TITLE);
    expect(notices[1]?.text).toBe(PPE_NITRILE_TEXT);
    expect(notices[1]?.images).toHaveLength(1);
  });

  it('e0000029 (пом’якшуючий вкладиш) несе лише full-kit з 4 фото', () => {
    const notices = getPpeNotices(STEP_SOFT_LINER);
    expect(notices.map((n) => n.kind)).toEqual(['full-kit']);
    expect(notices[0]?.images.map((i) => i.src)).toEqual([
      '/ppe/heat-resistant-gloves.png',
      '/ppe/safety-glasses.png',
      '/ppe/respirator.png',
      '/ppe/earmuffs.png',
    ]);
  });

  it.each([
    STEP_PLASTER_POSITIVE,
    STEP_TRAINING_FITTING,
    STEP_TRAINING_CORRECTION,
    STEP_PERMANENT_CORRECTION,
  ])('%s несе лише nitrile з 1 фото', (stepId) => {
    const notices = getPpeNotices(stepId);
    expect(notices.map((n) => n.kind)).toEqual(['nitrile']);
    expect(notices[0]?.title).toBe(PPE_NITRILE_TITLE);
    expect(notices[0]?.text).toBe(PPE_NITRILE_TEXT);
    expect(notices[0]?.images).toEqual([
      {
        src: '/ppe/nitrile-gloves.png',
        alt: expect.any(String),
        caption: 'Нітрилові рукавиці',
      },
    ]);
  });

  it('кожне фото має непорожні src/alt/caption', () => {
    for (const notices of Object.values(PPE_NOTICES_BY_STEP)) {
      for (const notice of notices) {
        expect(notice.images.length).toBeGreaterThan(0);
        for (const img of notice.images) {
          expect(img.src.trim()).not.toBe('');
          expect(img.alt.trim()).not.toBe('');
          expect(img.caption.trim()).not.toBe('');
          expect(img.src.startsWith('/ppe/')).toBe(true);
        }
      }
    }
  });

  it.each([
    'e0000020-0000-0000-0000-000000000020', // Етап 1, КРОК 1 — заміри
    'e0000021-0000-0000-0000-000000000021', // Етап 1, КРОК 2 — гіпсовий негатив (без повідомлень)
    'e0000023-0000-0000-0000-000000000023', // Етап 2, КРОК 2 — перевірка позитива
    'e0000025-0000-0000-0000-000000000025', // Етап 3, КРОК 2 — контроль якості гільзи
    'e0000030-0000-0000-0000-000000000030', // Етап 7, КРОК 2 — постійна гільза
    'e0000031-0000-0000-0000-000000000031', // Етап 8 — складання постійного протеза
    'e0000033-0000-0000-0000-000000000033', // Етап 10 — видача протеза
    'e0000041-0000-0000-0000-000000000001', // TP-UL-01 термоформування (власний PPE-блок)
    'non-existent-step',
  ])('крок %s не має нотисів', (stepId) => {
    expect(getPpeNotices(stepId)).toEqual([]);
  });

  it('undefined/null → []', () => {
    expect(getPpeNotices(undefined)).toEqual([]);
    expect(getPpeNotices(null)).toEqual([]);
  });
});
