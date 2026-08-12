import type { CSSProperties } from 'react';

/**
 * Measurement forms for the «Зняття мірок (з пацієнтом)» step — a pixel-perfect
 * replication of the reference forms (pixel-perfect-forms-main, served at :8080).
 *
 * Sheet 1 (295×523): full-arm diagram, 5 circumference circles + 5 length squares.
 * Sheet 2 (343×264): epicondyle-detail diagram, 2 squares + 5 circles.
 *
 * Transition rule: the step completes when at least MIN_MEASUREMENT_VALUES fields
 * are filled (enforced in the wizard button gating and by the backend validator).
 */

export const MIN_MEASUREMENT_VALUES = 3;

/** Step display names (current + legacy snapshot) that render the measurement forms. */
export const MEASUREMENT_FORMS_STEP_NAMES = new Set(['Вимірювання кукси', 'Зняття мірок (з пацієнтом)']);

interface MeasurementFieldDef {
  key: string;
  label: string;
  shape: 'circle' | 'square';
  top: number;
  left: number;
  w: number;
  h: number;
}

const FORM_1_FIELDS: MeasurementFieldDef[] = [
  { key: 'chest_circumference', label: 'Обхват грудей', shape: 'circle', top: 4, left: 23, w: 51, h: 50 },
  { key: 'axilla_circumference', label: 'Обхват пахви', shape: 'circle', top: 111, left: 81, w: 52, h: 50 },
  { key: 'epicondyle_circumference', label: 'Обхват надвиростків', shape: 'circle', top: 242, left: 81, w: 52, h: 50 },
  { key: 'forearm_circumference', label: 'Обхват передпліччя', shape: 'circle', top: 336, left: 81, w: 52, h: 50 },
  { key: 'styloid_circumference', label: 'Обхват шилоподібного відростка', shape: 'circle', top: 400, left: 81, w: 52, h: 50 },
  { key: 'acromion_to_epicondyle', label: 'Акроміон — надвиросток', shape: 'square', top: 178, left: 23, w: 50, h: 48 },
  { key: 'epicondyle_to_thumb_tip', label: 'Надвиросток — кінчик великого пальця', shape: 'square', top: 336, left: 23, w: 50, h: 48 },
  { key: 'axilla_to_epicondyle', label: 'Пахва — надвиросток', shape: 'square', top: 178, left: 223, w: 50, h: 48 },
  { key: 'epicondyle_to_styloid', label: 'Надвиросток — шилоподібний відросток', shape: 'square', top: 336, left: 223, w: 50, h: 48 },
  { key: 'styloid_to_thumb_tip', label: 'Шилоподібний відросток — кінчик великого пальця', shape: 'square', top: 437, left: 223, w: 50, h: 48 },
];

const FORM_2_FIELDS: MeasurementFieldDef[] = [
  { key: 'forearm_width', label: 'Ширина передпліччя', shape: 'square', top: 61, left: 66, w: 56, h: 56 },
  { key: 'cast_height_at_epicondyle', label: 'Висота гіпсу на надвиростку', shape: 'square', top: 61, left: 225, w: 57, h: 56 },
  { key: 'proximal_circumference', label: 'Проксимальний обхват', shape: 'circle', top: 4, left: 280, w: 57, h: 57 },
  { key: 'distal_circumference', label: 'Дистальний обхват', shape: 'circle', top: 110, left: 280, w: 57, h: 58 },
  { key: 'distal_forearm_circumference', label: 'Дистальний обхват передпліччя', shape: 'circle', top: 204, left: 6, w: 58, h: 59 },
  { key: 'mid_forearm_circumference', label: 'Обхват середньої третини передпліччя', shape: 'circle', top: 205, left: 64, w: 58, h: 58 },
  { key: 'proximal_forearm_circumference', label: 'Проксимальний обхват передпліччя', shape: 'circle', top: 204, left: 125, w: 58, h: 59 },
];

const ALL_FIELDS = [...FORM_1_FIELDS, ...FORM_2_FIELDS];

const INK = 'oklch(0.32 0.008 260)';
const ACCENT = 'oklch(0.62 0.13 55)';

const fieldStyle = (f: MeasurementFieldDef): CSSProperties => ({
  position: 'absolute',
  top: f.top,
  left: f.left,
  width: f.w,
  height: f.h,
  margin: 0,
  padding: 0,
  background: 'oklch(1 0 0)',
  border: `1px solid ${INK}`,
  color: INK,
  fontFamily: '"Segoe UI", "Helvetica Neue", Arial, sans-serif',
  fontSize: 15,
  lineHeight: '15px',
  textAlign: 'center',
  borderRadius: f.shape === 'circle' ? '50%' : 0,
  outline: 'none',
  boxSizing: 'border-box',
});

/** Number of measurement fields with a non-blank value (transition rule input). */
export function countFilledMeasurementValues(values: Record<string, unknown>): number {
  return ALL_FIELDS.filter((f) => {
    const v = values[f.key];
    return typeof v === 'string' && v.trim() !== '';
  }).length;
}

export function MeasurementForms({
  values,
  onChange,
  disabled = false,
}: {
  values: Record<string, unknown>;
  onChange: (key: string, value: string) => void;
  disabled?: boolean;
}) {
  const renderSheet = (
    image: string,
    alt: string,
    w: number,
    h: number,
    fields: MeasurementFieldDef[],
  ) => (
    <div
      className="measure-sheet measurement-sheet"
      style={{ position: 'relative', width: w, height: h, lineHeight: 0 }}
    >
      <img src={image} alt={alt} width={w} height={h} draggable={false} />
      {fields.map((f) => (
        <input
          key={f.key}
          type="text"
          inputMode="decimal"
          className="measurement-field"
          aria-label={f.label}
          value={(values[f.key] as string) ?? ''}
          disabled={disabled}
          onChange={(e) => onChange(f.key, e.target.value)}
          style={fieldStyle(f)}
        />
      ))}
    </div>
  );

  return (
    <div className="flex flex-col items-start gap-8 md:flex-row md:gap-10">
      <style>{`
        .measurement-field:focus {
          border-color: ${ACCENT} !important;
          box-shadow: 0 0 0 1px ${ACCENT};
        }
      `}</style>
      {renderSheet(
        '/measurement/form1.png',
        'Схема вимірювання руки: кола обхватів та квадрати довжин',
        295,
        523,
        FORM_1_FIELDS,
      )}
      <div
        aria-hidden="true"
        className="h-px w-full bg-border md:h-auto md:w-px md:self-stretch"
      />
      {renderSheet(
        '/measurement/form2.png',
        'Схема вимірювання ділянки надвиростків',
        343,
        264,
        FORM_2_FIELDS,
      )}
    </div>
  );
}
