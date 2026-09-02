export const FAILURE_CATEGORIES: { value: string; label: string }[] = [
  { value: 'defect', label: 'Виробничий дефект' },
  { value: 'materials', label: 'Проблеми з матеріалами' },
  { value: 'component_damage', label: 'Пошкодження компонента' },
  { value: 'order_cancelled', label: 'Скасування замовлення пацієнтом' },
  { value: 'patient', label: 'Проблеми з пацієнтом' },
  { value: 'other', label: 'Інше' },
];

// Legacy display for QC failures (FAILED_QC) that still store category="quality_gate" internally.
// Not present in FAILURE_CATEGORIES (not selectable in UI) but kept for FailedScreen rendering.
export const FAILURE_CATEGORY_LABELS: Record<string, string> = {
  ...Object.fromEntries(FAILURE_CATEGORIES.map((c) => [c.value, c.label])),
  quality_gate: 'Повторна невдача на Quality Gate',
};