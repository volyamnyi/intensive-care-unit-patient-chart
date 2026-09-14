/**
 * Інформаційні повідомлення про обов'язкові засоби індивідуального захисту (ЗІЗ)
 * у візарді «Виробництво протезів» (епік PPE, #293).
 *
 * Єдине джерело правил відображення: ключ — `stepId` кроку TP-LL-02
 * (назви етапів/кроків рядками ніде не дублюються). Один крок може нести
 * декілька повідомлень (напр. `e0000024` — комплект + нітрил); порядок
 * у масиві = порядок рендеру (`full-kit` завжди першим).
 *
 * Повідомлення суто інформаційні (non-blocking): не додають елементів,
 * не впливають на валідацію `completeStep`, бекенд/seed не зачеплені.
 * Точка рендеру — `WizardScreen` (`getPpeNotices(step?.id)` у `step-fade-in`
 * обгортці, #294).
 */

export type PpeNoticeKind = 'full-kit' | 'nitrile';

export interface PpeNoticeImage {
  /** Шлях у Vite public dir, напр. `/ppe/respirator.png`. */
  src: string;
  /** Змістовний alt українською (a11y). */
  alt: string;
  /** Короткий підпис під фото (`<figcaption>`). */
  caption: string;
}

export interface PpeNotice {
  kind: PpeNoticeKind;
  /** Заголовок банера (`AlertTitle`). */
  title: string;
  /** Точний текст попередження (`AlertDescription`). */
  text: string;
  images: PpeNoticeImage[];
}

export const PPE_FULL_KIT_TITLE = 'Засоби індивідуального захисту';

export const PPE_FULL_KIT_TEXT =
  'Роботу виконувати із застосуванням засобів індивідуального захисту (термостійкі рукавиці, окуляри, респіратор, навушники)!!!';

export const PPE_NITRILE_TITLE = 'Захист рук';

export const PPE_NITRILE_TEXT =
  'Роботу виконувати у штучних захисних рукавицях (нітрилові рукавиці)!!!';

const FULL_KIT_IMAGES: PpeNoticeImage[] = [
  {
    src: '/ppe/heat-resistant-gloves.png',
    alt: 'Термостійкі рукавиці — засіб індивідуального захисту рук від термічних ризиків',
    caption: 'Термостійкі рукавиці',
  },
  {
    src: '/ppe/safety-glasses.png',
    alt: 'Захисні окуляри — засіб індивідуального захисту очей',
    caption: 'Захисні окуляри',
  },
  {
    src: '/ppe/respirator.png',
    alt: 'Респіратор — засіб індивідуального захисту органів дихання',
    caption: 'Респіратор',
  },
  {
    src: '/ppe/earmuffs.png',
    alt: 'Захисні навушники — засіб індивідуального захисту органів слуху',
    caption: 'Навушники',
  },
];

const NITRILE_IMAGES: PpeNoticeImage[] = [
  {
    src: '/ppe/nitrile-gloves.png',
    alt: 'Нітрилові рукавиці — штучні захисні рукавиці для захисту рук',
    caption: 'Нітрилові рукавиці',
  },
];

const FULL_KIT_NOTICE: PpeNotice = {
  kind: 'full-kit',
  title: PPE_FULL_KIT_TITLE,
  text: PPE_FULL_KIT_TEXT,
  images: FULL_KIT_IMAGES,
};

const NITRILE_NOTICE: PpeNotice = {
  kind: 'nitrile',
  title: PPE_NITRILE_TITLE,
  text: PPE_NITRILE_TEXT,
  images: NITRILE_IMAGES,
};

// TP-LL-02 stepId — див. `data-prosth.sql` (шаблон `c0000003`).
// Етап 3 / КРОК 1 «Виготовлення тренувальної гільзи».
const STEP_TRAINING_SOCKET = 'e0000024-0000-0000-0000-000000000024';
// Етап 7 / КРОК 1 «Виготовлення пом'якшуючого вкладиша».
const STEP_SOFT_LINER = 'e0000029-0000-0000-0000-000000000029';
// Етап 2 / КРОК 1 «Виготовлення гіпсового позитива».
const STEP_PLASTER_POSITIVE = 'e0000022-0000-0000-0000-000000000022';
// Етап 4 / КРОК 1 «Примірка тренувальної гільзи».
const STEP_TRAINING_FITTING = 'e0000026-0000-0000-0000-000000000026';
// Етап 6 / КРОК 1 «Примірювання та коректування тренувального протеза».
const STEP_TRAINING_CORRECTION = 'e0000028-0000-0000-0000-000000000028';
// Етап 9 / КРОК 1 «Примірювання та коректування постійного протеза».
const STEP_PERMANENT_CORRECTION = 'e0000032-0000-0000-0000-000000000032';

export const PPE_NOTICES_BY_STEP: Record<string, PpeNotice[]> = {
  [STEP_TRAINING_SOCKET]: [FULL_KIT_NOTICE, NITRILE_NOTICE],
  [STEP_SOFT_LINER]: [FULL_KIT_NOTICE],
  [STEP_PLASTER_POSITIVE]: [NITRILE_NOTICE],
  [STEP_TRAINING_FITTING]: [NITRILE_NOTICE],
  [STEP_TRAINING_CORRECTION]: [NITRILE_NOTICE],
  [STEP_PERMANENT_CORRECTION]: [NITRILE_NOTICE],
};

/** Нотиси ЗІЗ для кроку; невідомий/відсутній `stepId` → `[]` (банера немає). */
export const getPpeNotices = (stepId: string | undefined | null): PpeNotice[] =>
  (stepId ? PPE_NOTICES_BY_STEP[stepId] : undefined) ?? [];
