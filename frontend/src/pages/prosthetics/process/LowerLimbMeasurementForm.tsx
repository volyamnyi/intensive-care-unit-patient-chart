import * as React from 'react';

/**
 * Lower-limb measurement form — pixel-perfect adaptation of measurement-master
 * (http://localhost:8080, C:\projects\intensive-care-unit-patient-chart\measurement-master-main)
 * for TP-LL-02 step «Зняття та внесення об''ємних розмірів» (e0000020).
 *
 * Structure mirrors the reference:
 *  - header "Бланк замірів №" + numbered input
 *  - section grid of patient/product fields
 *  - diagram "Об''ємний розмір та довжина кукси" with 30 positioned numeric inputs
 *  - bottom grid (heel height, foot size, components)
 *  - print footer
 *
 * Values are stored under the backend element UUIDs defined in data-prosth.sql
 * (f0000300…f0000344, f0000200…f0000203 legacy). Diagram positions are taken
 * verbatim from the reference BOXES (797×764).
 */

export const LOWER_LIMB_STEP_ID = 'e0000020-0000-0000-0000-000000000020';

// Header / form field element IDs (mirroring data-prosth.sql)
export const LOWER_LIMB_ELEMENT_IDS = {
  blankNumber: 'f0000300-0000-0000-0000-000000000300',
  date: 'f0000301-0000-0000-0000-000000000301',
  pib: 'f0000302-0000-0000-0000-000000000302',
  address: 'f0000303-0000-0000-0000-000000000303',
  productCode: 'f0000304-0000-0000-0000-000000000304',
  productName: 'f0000305-0000-0000-0000-000000000305',
  mobilityLevel: 'f0000306-0000-0000-0000-000000000306',
  gender: 'f0000307-0000-0000-0000-000000000307',
  age: 'f0000308-0000-0000-0000-000000000308',
  height: 'f0000309-0000-0000-0000-000000000309',
  weight: 'f0000310-0000-0000-0000-000000000310',
  notes: 'f0000311-0000-0000-0000-000000000311',
  // diagram 30
  a_r: 'f0000312-0000-0000-0000-000000000312',
  a_l: 'f0000313-0000-0000-0000-000000000313',
  a_left_1: 'f0000314-0000-0000-0000-000000000314',
  a_left_2: 'f0000315-0000-0000-0000-000000000315',
  a_left_3: 'f0000316-0000-0000-0000-000000000316',
  a_left_4: 'f0000317-0000-0000-0000-000000000317',
  a_right_1: 'f0000318-0000-0000-0000-000000000318',
  a_right_2: 'f0000319-0000-0000-0000-000000000319',
  a_right_3: 'f0000320-0000-0000-0000-000000000320',
  a_right_4: 'f0000321-0000-0000-0000-000000000321',
  b_calf: 'f0000322-0000-0000-0000-000000000322',
  b_ankle: 'f0000323-0000-0000-0000-000000000323',
  b_len_r: 'f0000324-0000-0000-0000-000000000324',
  b_len_l: 'f0000325-0000-0000-0000-000000000325',
  b_foot: 'f0000326-0000-0000-0000-000000000326',
  c_r: 'f0000327-0000-0000-0000-000000000327',
  c_l: 'f0000328-0000-0000-0000-000000000328',
  c_left_1: 'f0000329-0000-0000-0000-000000000329',
  c_left_2: 'f0000330-0000-0000-0000-000000000330',
  c_right_1: 'f0000331-0000-0000-0000-000000000331',
  c_right_2: 'f0000332-0000-0000-0000-000000000332',
  c_len: 'f0000333-0000-0000-0000-000000000333',
  d_r_25: 'f0000334-0000-0000-0000-000000000334',
  d_l_25: 'f0000335-0000-0000-0000-000000000335',
  d_r_5: 'f0000336-0000-0000-0000-000000000336',
  d_l_5: 'f0000337-0000-0000-0000-000000000337',
  d_r_10: 'f0000338-0000-0000-0000-000000000338',
  d_l_10: 'f0000339-0000-0000-0000-000000000339',
  d_r_15: 'f0000340-0000-0000-0000-000000000340',
  d_l_15: 'f0000341-0000-0000-0000-000000000341',
  heelHeight: 'f0000342-0000-0000-0000-000000000342',
  footSize: 'f0000343-0000-0000-0000-000000000343',
  components: 'f0000344-0000-0000-0000-000000000344',
  // legacy (kept for backward compat, not rendered)
  legacy_length: 'f0000200-0000-0000-0000-000000000200',
  legacy_circ: 'f0000201-0000-0000-0000-000000000201',
  legacy_5cm: 'f0000202-0000-0000-0000-000000000202',
  legacy_10cm: 'f0000203-0000-0000-0000-000000000203',
} as const;

export const LOWER_LIMB_DIAGRAM_IDS = [
  LOWER_LIMB_ELEMENT_IDS.a_r,
  LOWER_LIMB_ELEMENT_IDS.a_l,
  LOWER_LIMB_ELEMENT_IDS.a_left_1,
  LOWER_LIMB_ELEMENT_IDS.a_left_2,
  LOWER_LIMB_ELEMENT_IDS.a_left_3,
  LOWER_LIMB_ELEMENT_IDS.a_left_4,
  LOWER_LIMB_ELEMENT_IDS.a_right_1,
  LOWER_LIMB_ELEMENT_IDS.a_right_2,
  LOWER_LIMB_ELEMENT_IDS.a_right_3,
  LOWER_LIMB_ELEMENT_IDS.a_right_4,
  LOWER_LIMB_ELEMENT_IDS.b_calf,
  LOWER_LIMB_ELEMENT_IDS.b_ankle,
  LOWER_LIMB_ELEMENT_IDS.b_len_r,
  LOWER_LIMB_ELEMENT_IDS.b_len_l,
  LOWER_LIMB_ELEMENT_IDS.b_foot,
  LOWER_LIMB_ELEMENT_IDS.c_r,
  LOWER_LIMB_ELEMENT_IDS.c_l,
  LOWER_LIMB_ELEMENT_IDS.c_left_1,
  LOWER_LIMB_ELEMENT_IDS.c_left_2,
  LOWER_LIMB_ELEMENT_IDS.c_right_1,
  LOWER_LIMB_ELEMENT_IDS.c_right_2,
  LOWER_LIMB_ELEMENT_IDS.c_len,
  LOWER_LIMB_ELEMENT_IDS.d_r_25,
  LOWER_LIMB_ELEMENT_IDS.d_l_25,
  LOWER_LIMB_ELEMENT_IDS.d_r_5,
  LOWER_LIMB_ELEMENT_IDS.d_l_5,
  LOWER_LIMB_ELEMENT_IDS.d_r_10,
  LOWER_LIMB_ELEMENT_IDS.d_l_10,
  LOWER_LIMB_ELEMENT_IDS.d_r_15,
  LOWER_LIMB_ELEMENT_IDS.d_l_15,
] as const;

export const LOWER_LIMB_HEADER_IDS = [
  LOWER_LIMB_ELEMENT_IDS.blankNumber,
  LOWER_LIMB_ELEMENT_IDS.date,
  LOWER_LIMB_ELEMENT_IDS.pib,
  LOWER_LIMB_ELEMENT_IDS.address,
  LOWER_LIMB_ELEMENT_IDS.productCode,
  LOWER_LIMB_ELEMENT_IDS.productName,
  LOWER_LIMB_ELEMENT_IDS.mobilityLevel,
  LOWER_LIMB_ELEMENT_IDS.gender,
  LOWER_LIMB_ELEMENT_IDS.age,
  LOWER_LIMB_ELEMENT_IDS.height,
  LOWER_LIMB_ELEMENT_IDS.weight,
  LOWER_LIMB_ELEMENT_IDS.notes,
] as const;

export const LOWER_LIMB_BOTTOM_IDS = [
  LOWER_LIMB_ELEMENT_IDS.heelHeight,
  LOWER_LIMB_ELEMENT_IDS.footSize,
  LOWER_LIMB_ELEMENT_IDS.components,
] as const;

export const ALL_LOWER_LIMB_IDS = [
  ...LOWER_LIMB_HEADER_IDS,
  ...LOWER_LIMB_DIAGRAM_IDS,
  ...LOWER_LIMB_BOTTOM_IDS,
] as const;

const IMG_W = 797;
const IMG_H = 764;

type Box = { id: string; elementId: string; x: number; y: number; w: number; h: number; label: string };

const BOXES: Box[] = [
  { id: 'a_r', elementId: LOWER_LIMB_ELEMENT_IDS.a_r, x: 156, y: 61, w: 46, h: 30, label: 'Стегно, R' },
  { id: 'a_l', elementId: LOWER_LIMB_ELEMENT_IDS.a_l, x: 216, y: 61, w: 46, h: 30, label: 'Стегно, L' },
  { id: 'a_left_1', elementId: LOWER_LIMB_ELEMENT_IDS.a_left_1, x: 79, y: 196, w: 41, h: 27, label: 'Стегно, лівий вимір 1' },
  { id: 'a_left_2', elementId: LOWER_LIMB_ELEMENT_IDS.a_left_2, x: 79, y: 231, w: 41, h: 27, label: 'Стегно, лівий вимір 2' },
  { id: 'a_left_3', elementId: LOWER_LIMB_ELEMENT_IDS.a_left_3, x: 79, y: 268, w: 41, h: 28, label: 'Стегно, лівий вимір 3' },
  { id: 'a_left_4', elementId: LOWER_LIMB_ELEMENT_IDS.a_left_4, x: 79, y: 306, w: 41, h: 28, label: 'Стегно, лівий вимір 4' },
  { id: 'a_right_1', elementId: LOWER_LIMB_ELEMENT_IDS.a_right_1, x: 282, y: 211, w: 38, h: 30, label: 'Стегно, правий вимір 1' },
  { id: 'a_right_2', elementId: LOWER_LIMB_ELEMENT_IDS.a_right_2, x: 277, y: 251, w: 43, h: 30, label: 'Стегно, правий вимір 2' },
  { id: 'a_right_3', elementId: LOWER_LIMB_ELEMENT_IDS.a_right_3, x: 277, y: 289, w: 43, h: 30, label: 'Стегно, правий вимір 3' },
  { id: 'a_right_4', elementId: LOWER_LIMB_ELEMENT_IDS.a_right_4, x: 277, y: 327, w: 43, h: 30, label: 'Стегно, правий вимір 4' },
  { id: 'b_calf', elementId: LOWER_LIMB_ELEMENT_IDS.b_calf, x: 427, y: 284, w: 45, h: 28, label: 'Обхват гомілки' },
  { id: 'b_ankle', elementId: LOWER_LIMB_ELEMENT_IDS.b_ankle, x: 427, y: 401, w: 45, h: 27, label: 'Обхват щиколотки' },
  { id: 'b_len_r', elementId: LOWER_LIMB_ELEMENT_IDS.b_len_r, x: 700, y: 320, w: 40, h: 26, label: 'Довжина кінцівки, R' },
  { id: 'b_len_l', elementId: LOWER_LIMB_ELEMENT_IDS.b_len_l, x: 700, y: 366, w: 40, h: 26, label: 'Довжина кінцівки, L' },
  { id: 'b_foot', elementId: LOWER_LIMB_ELEMENT_IDS.b_foot, x: 625, y: 467, w: 47, h: 28, label: 'Висота стопи' },
  { id: 'c_r', elementId: LOWER_LIMB_ELEMENT_IDS.c_r, x: 137, y: 420, w: 46, h: 28, label: 'Коліно, R' },
  { id: 'c_l', elementId: LOWER_LIMB_ELEMENT_IDS.c_l, x: 196, y: 420, w: 47, h: 28, label: 'Коліно, L' },
  { id: 'c_left_1', elementId: LOWER_LIMB_ELEMENT_IDS.c_left_1, x: 53, y: 510, w: 47, h: 26, label: 'Коліно, лівий вимір 1' },
  { id: 'c_left_2', elementId: LOWER_LIMB_ELEMENT_IDS.c_left_2, x: 53, y: 545, w: 47, h: 28, label: 'Коліно, лівий вимір 2' },
  { id: 'c_right_1', elementId: LOWER_LIMB_ELEMENT_IDS.c_right_1, x: 282, y: 510, w: 48, h: 26, label: 'Коліно, правий вимір 1' },
  { id: 'c_right_2', elementId: LOWER_LIMB_ELEMENT_IDS.c_right_2, x: 282, y: 545, w: 48, h: 28, label: 'Коліно, правий вимір 2' },
  { id: 'c_len', elementId: LOWER_LIMB_ELEMENT_IDS.c_len, x: 282, y: 633, w: 48, h: 28, label: 'Коліно, довжина' },
  { id: 'd_r_25', elementId: LOWER_LIMB_ELEMENT_IDS.d_r_25, x: 683, y: 652, w: 44, h: 20, label: 'Таз R, рівень 2,5' },
  { id: 'd_l_25', elementId: LOWER_LIMB_ELEMENT_IDS.d_l_25, x: 731, y: 652, w: 45, h: 20, label: 'Таз L, рівень 2,5' },
  { id: 'd_r_5', elementId: LOWER_LIMB_ELEMENT_IDS.d_r_5, x: 683, y: 674, w: 44, h: 26, label: 'Таз R, рівень 5' },
  { id: 'd_l_5', elementId: LOWER_LIMB_ELEMENT_IDS.d_l_5, x: 731, y: 674, w: 45, h: 26, label: 'Таз L, рівень 5' },
  { id: 'd_r_10', elementId: LOWER_LIMB_ELEMENT_IDS.d_r_10, x: 683, y: 702, w: 44, h: 27, label: 'Таз R, рівень 10' },
  { id: 'd_l_10', elementId: LOWER_LIMB_ELEMENT_IDS.d_l_10, x: 731, y: 702, w: 45, h: 27, label: 'Таз L, рівень 10' },
  { id: 'd_r_15', elementId: LOWER_LIMB_ELEMENT_IDS.d_r_15, x: 683, y: 731, w: 44, h: 27, label: 'Таз R, рівень 15' },
  { id: 'd_l_15', elementId: LOWER_LIMB_ELEMENT_IDS.d_l_15, x: 731, y: 731, w: 45, h: 27, label: 'Таз L, рівень 15' },
];

const pct = (v: number, total: number) => `${(v / total) * 100}%`;

function Field({
  label,
  children,
  suffix,
  className = '',
}: {
  label: string;
  children: React.ReactNode;
  suffix?: string;
  className?: string;
}) {
  return (
    <label className={`block min-w-0 ${className}`}>
      <span className="mb-1.5 block text-xs font-semibold tracking-wide text-muted-foreground uppercase">{label}</span>
      <span className="flex items-center gap-2">
        {children}
        {suffix ? <span className="shrink-0 text-sm text-muted-foreground">{suffix}</span> : null}
      </span>
    </label>
  );
}

export function countFilledLowerLimbDiagram(values: Record<string, unknown>): number {
  return LOWER_LIMB_DIAGRAM_IDS.filter((id) => {
    const v = values[id];
    return typeof v === 'string' ? v.trim() !== '' : v !== undefined && v !== null && String(v).trim() !== '';
  }).length;
}

export function countFilledLowerLimbAll(values: Record<string, unknown>): number {
  return ALL_LOWER_LIMB_IDS.filter((id) => {
    const v = values[id];
    return typeof v === 'string' ? v.trim() !== '' : v !== undefined && v !== null && String(v).trim() !== '';
  }).length;
}

export function LowerLimbMeasurementForm({
  values,
  onChange,
  errors = {},
  disabled = false,
}: {
  values: Record<string, unknown>;
  onChange: (id: string, value: unknown) => void;
  errors?: Record<string, string>;
  disabled?: boolean;
}) {
  const getVal = (id: string) => (values[id] as string) ?? '';
  const setVal = (id: string, v: string) => onChange(id, v);

  const numericFilter = (v: string) => v.replace(/[^\d.,]/g, '');

  return (
    <div className="mx-auto w-full max-w-5xl overflow-hidden rounded-xl border border-border bg-card shadow-sm">
      <header className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-4 border-b border-border bg-secondary px-4 py-5 sm:px-8">
        <h1 className="min-w-0 truncate text-lg font-bold tracking-tight text-secondary-foreground sm:text-2xl">Бланк замірів №</h1>
        <input
          type="number"
          inputMode="numeric"
          aria-label="Номер бланку замірів"
          placeholder="№"
          value={getVal(LOWER_LIMB_ELEMENT_IDS.blankNumber)}
          disabled={disabled}
          onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.blankNumber, numericFilter(e.target.value))}
          className={`field-input w-24 shrink-0 text-center font-semibold sm:w-32 ${errors[LOWER_LIMB_ELEMENT_IDS.blankNumber] ? 'border-destructive ring-1 ring-destructive' : ''}`}
        />
      </header>

      <section className="grid grid-cols-1 gap-4 px-4 py-6 sm:grid-cols-2 sm:px-8 lg:grid-cols-3">
        <Field label="Дата">
          <input
            type="date"
            aria-label="Дата"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.date)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.date, e.target.value)}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.date] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
        <Field label="П.І.Б" className="sm:col-span-1 lg:col-span-2">
          <input
            type="text"
            aria-label="П.І.Б"
            placeholder="П.І.Б"
            autoComplete="name"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.pib)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.pib, e.target.value)}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.pib] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
        <Field label="Адреса" className="sm:col-span-2 lg:col-span-3">
          <input
            type="text"
            aria-label="Адреса"
            placeholder="Адреса"
            autoComplete="street-address"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.address)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.address, e.target.value)}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.address] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
        <Field label="Шифр виробу">
          <input
            type="text"
            aria-label="Шифр виробу"
            placeholder="Шифр"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.productCode)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.productCode, e.target.value)}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.productCode] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
        <Field label="Найменування виробу" className="lg:col-span-2">
          <input
            type="text"
            aria-label="Найменування виробу"
            placeholder="Найменування"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.productName)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.productName, e.target.value)}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.productName] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
        <Field label="Рівень мобільності">
          <input
            type="text"
            aria-label="Рівень мобільності"
            placeholder="Рівень"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.mobilityLevel)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.mobilityLevel, e.target.value)}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.mobilityLevel] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
        <Field label="Стать">
          <select
            aria-label="Стать"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.gender)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.gender, e.target.value)}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.gender] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          >
            <option value="" disabled>
              Оберіть…
            </option>
            <option value="Чоловіча">Чоловіча</option>
            <option value="Жіноча">Жіноча</option>
          </select>
        </Field>
        <Field label="Вік" suffix="років">
          <input
            type="number"
            inputMode="numeric"
            min={0}
            max={120}
            aria-label="Вік"
            placeholder="0"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.age)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.age, numericFilter(e.target.value))}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.age] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
        <Field label="Зріст" suffix="см">
          <input
            type="number"
            inputMode="decimal"
            min={0}
            aria-label="Зріст"
            placeholder="0"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.height)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.height, numericFilter(e.target.value))}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.height] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
        <Field label="Вага" suffix="кг">
          <input
            type="number"
            inputMode="decimal"
            min={0}
            aria-label="Вага"
            placeholder="0"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.weight)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.weight, numericFilter(e.target.value))}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.weight] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
        <Field label="Примітки" className="sm:col-span-2 lg:col-span-1">
          <input
            type="text"
            aria-label="Примітки"
            placeholder="Примітки"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.notes)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.notes, e.target.value)}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.notes] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
      </section>

      <section className="border-t border-border px-2 py-6 sm:px-8">
        <h2 className="mb-4 px-2 text-sm font-bold tracking-wide text-foreground uppercase sm:px-0">Об&apos;ємний розмір та довжина кукси</h2>
        <div className="-mx-2 overflow-x-auto px-2 pb-2 sm:mx-0 sm:px-0">
          <div className="relative mx-auto min-w-[680px] select-none" style={{ containerType: 'inline-size', maxWidth: `${IMG_W}px` } as React.CSSProperties}>
            <img
              src="/measurement/lower-limb-diagram.jpg"
              alt="Схема замірів кукси та нижніх кінцівок"
              width={IMG_W}
              height={IMG_H}
              className="block h-auto w-full"
              draggable={false}
            />
            {BOXES.map((b) => {
              const hasError = !!errors[b.elementId];
              return (
                <input
                  key={b.id}
                  type="number"
                  inputMode="decimal"
                  aria-label={b.label}
                  title={b.label}
                  value={getVal(b.elementId)}
                  disabled={disabled}
                  onChange={(e) => setVal(b.elementId, numericFilter(e.target.value))}
                  className={`diagram-input ${hasError ? '!border-destructive !ring-1 !ring-destructive' : ''}`}
                  style={{
                    left: pct(b.x, IMG_W),
                    top: pct(b.y, IMG_H),
                    width: pct(b.w, IMG_W),
                    height: pct(b.h, IMG_H),
                  }}
                />
              );
            })}
          </div>
        </div>
        <p className="mt-3 px-2 text-xs text-muted-foreground sm:px-0">Кожне поле на схемі — числове значення заміру в сантиметрах.</p>
      </section>

      <section className="grid grid-cols-1 gap-4 border-t border-border px-4 py-6 sm:grid-cols-3 sm:px-8">
        <Field label="Висота каблука" suffix="см">
          <input
            type="number"
            inputMode="decimal"
            min={0}
            aria-label="Висота каблука"
            placeholder="0"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.heelHeight)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.heelHeight, numericFilter(e.target.value))}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.heelHeight] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
        <Field label="Розмір стопи" suffix="см">
          <input
            type="number"
            inputMode="decimal"
            min={0}
            aria-label="Розмір стопи"
            placeholder="0"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.footSize)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.footSize, numericFilter(e.target.value))}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.footSize] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
        <Field label="Комплектуючі">
          <input
            type="text"
            aria-label="Комплектуючі"
            placeholder="Комплектуючі"
            value={getVal(LOWER_LIMB_ELEMENT_IDS.components)}
            disabled={disabled}
            onChange={(e) => setVal(LOWER_LIMB_ELEMENT_IDS.components, e.target.value)}
            className={`field-input ${errors[LOWER_LIMB_ELEMENT_IDS.components] ? 'border-destructive ring-1 ring-destructive' : ''}`}
          />
        </Field>
      </section>

      <footer className="flex flex-wrap items-center justify-end gap-3 border-t border-border bg-secondary px-4 py-4 sm:px-8">
        <button
          type="button"
          onClick={() => window.print()}
          className="rounded-md border border-input bg-card px-4 py-2.5 text-sm font-semibold text-foreground transition-colors hover:bg-muted"
        >
          Друк
        </button>
        <span className="text-xs text-muted-foreground">Заповніть не менше 3 вимірів на схемі для переходу далі.</span>
      </footer>
    </div>
  );
}
