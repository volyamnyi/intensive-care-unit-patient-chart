export const FAILURE_CATEGORIES: { value: string; label: string }[] = [
  { value: 'defect', label: 'Виробничий дефект' },
  { value: 'materials', label: 'Проблеми з матеріалами' },
  { value: 'quality_gate', label: 'Повторна невдача на Quality Gate' },
  { value: 'component_damage', label: 'Пошкодження компонента' },
  { value: 'order_cancelled', label: 'Скасування замовлення пацієнтом' },
  { value: 'patient', label: 'Проблеми з пацієнтом' },
  { value: 'other', label: 'Інше' },
];

export const FAILURE_CATEGORY_LABELS: Record<string, string> = Object.fromEntries(
  FAILURE_CATEGORIES.map((c) => [c.value, c.label]),
);