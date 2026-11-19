import type { SnapshotElement } from './types';

/**
 * Lower-limb side selection (TP-LL-02, stage 1, step e0000020). A checked box
 * means that side is the amputated/prosthetic side; at least one side must be
 * selected (R, L or both). Older template snapshots declare these same IDs as
 * NUMERIC_INPUT — they keep rendering numeric fields and are not validated by
 * this rule.
 */
export const LOWER_LIMB_SIDE_R_ID = 'f0000312-0000-0000-0000-000000000312';
export const LOWER_LIMB_SIDE_L_ID = 'f0000313-0000-0000-0000-000000000313';
export const LOWER_LIMB_SIDE_ERROR_KEY = 'lower-limb-sides';
export const LOWER_LIMB_SIDE_ERROR = 'Позначте бік кукси: R, L або обидва';

export function validateLowerLimbSides(
  elements: Pick<SnapshotElement, 'id' | 'elementType'>[],
  values: Record<string, unknown>,
): Record<string, string> {
  const sideR = elements.find((e) => e.id === LOWER_LIMB_SIDE_R_ID);
  const sideL = elements.find((e) => e.id === LOWER_LIMB_SIDE_L_ID);
  if (sideR?.elementType !== 'CHECKBOX' || sideL?.elementType !== 'CHECKBOX') return {};
  if (values[LOWER_LIMB_SIDE_R_ID] === true || values[LOWER_LIMB_SIDE_L_ID] === true) return {};
  return { [LOWER_LIMB_SIDE_ERROR_KEY]: LOWER_LIMB_SIDE_ERROR };
}

export function fmt(sec: number): string {
  const h = String(Math.floor(sec / 3600)).padStart(2, '0');
  const m = String(Math.floor((sec % 3600) / 60)).padStart(2, '0');
  const s = String(sec % 60).padStart(2, '0');
  return `${h}:${m}:${s}`;
}

export function computeProgress(stepsDone: number, totalSteps: number): number {
  return totalSteps > 0 ? Math.round((stepsDone / totalSteps) * 100) : 0;
}

export function validateElementValues(
  elements: SnapshotElement[],
  values: Record<string, unknown>,
): Record<string, string> {
  const map: Record<string, string> = {};
  elements.forEach((e) => {
    const v = values[e.id];
    if (e.required) {
      if (e.elementType === 'CHECKBOX' && v !== true) map[e.id] = "Обов'язкове підтвердження";
      else if (v === undefined || v === '' || v === null) map[e.id] = "Поле обов'язкове";
    }
    if (e.elementType === 'NUMERIC_INPUT' && v !== undefined && v !== '') {
      const n = Number(v);
      if (Number.isNaN(n)) map[e.id] = 'Введіть число';
      else if (e.minValue !== null && e.minValue !== undefined && n < e.minValue)
        map[e.id] = `Мінімум ${e.minValue} ${e.unit ?? ''}`;
      else if (e.maxValue !== null && e.maxValue !== undefined && n > e.maxValue)
        map[e.id] = `Максимум ${e.maxValue} ${e.unit ?? ''}`;
    }
    if (e.regexPattern && typeof v === 'string' && v !== '') {
      try {
        if (!new RegExp(e.regexPattern).test(v)) map[e.id] = 'Формат не відповідає вимогам';
      } catch {
        // invalid pattern — skip
      }
    }
    if (
      (e.elementType === 'TEXT_INPUT' || e.elementType === 'TEXTAREA') &&
      typeof v === 'string' &&
      v !== ''
    ) {
      if (e.minCount !== null && e.minCount !== undefined && v.length < e.minCount)
        map[e.id] = `Мінімум ${e.minCount} символів`;
      else if (e.maxCount !== null && e.maxCount !== undefined && v.length > e.maxCount)
        map[e.id] = `Максимум ${e.maxCount} символів`;
    }
  });
  return map;
}
