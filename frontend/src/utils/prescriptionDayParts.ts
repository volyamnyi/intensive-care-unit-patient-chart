import type { PrescriptionDayPart } from '../types/medication';

export function allDayPartsCompleted(dayParts: PrescriptionDayPart[]): boolean {
  return dayParts.length > 0 && dayParts.every((p) => p.isCompleted || p.isCompletedFinished);
}
