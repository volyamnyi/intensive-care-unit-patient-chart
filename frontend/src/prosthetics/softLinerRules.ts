export const SOFT_LINER_STEP_ID = 'e0000029-0000-0000-0000-000000000029';
export const VISUAL_SOFT_LINER_KEY = 'f0000214-0000-0000-0000-000000000214';
export const TACTILE_SOFT_LINER_KEY = 'f0000215-0000-0000-0000-000000000215';
export const NOT_REQUIRED_SOFT_LINER_KEY = 'f0000240-0000-0000-0000-000000000240';

export const SOFT_LINER_ERROR =
  'Для переходу виберіть або обидва контрольні чекбокси («Візуальний контроль чистоти помʼякшуючого вкладиша» та «Тактильний контроль поверхні…»), або чекбокс «Помʼякшуючий вкладиш не потрібен» — інші комбінації не дозволені';

export function isSoftLinerComboAllowed(
  visual: boolean,
  tactile: boolean,
  notRequired: boolean,
): boolean {
  return (visual && tactile && !notRequired) || (notRequired && !visual && !tactile);
}
