import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import HourlyGridDialog from '../../components/monitoring/HourlyGridDialog';
import type { ClinicalDay, Episode } from '../../types';

const selectedDay = {
  id: 'day-1', episodeId: 'ep-1', dayNumber: 1,
  startDateTime: '2025-06-01T08:00:00Z', endDateTime: '2025-06-02T08:00:00Z',
  status: 'OPEN', doctorSigned: false, nurseSigned: false, closedAt: null,
  weightKg: 72, bmi: null, createdBy: 1, createdAt: '',
  updatedBy: 0, updatedAt: '', version: 1,
} as ClinicalDay;

const episode = { id: 'ep-1', patientName: 'Тестовий Пацієнт' } as Episode;

function renderDialog(status: string) {
  return render(
    <HourlyGridDialog
      open
      onOpenChange={vi.fn()}
      episode={episode}
      selectedDay={{ ...selectedDay, status }}
      isLocked={false}
      saveStatus="saved"
    >
      <div>{'вміст карти'}</div>
    </HourlyGridDialog>
  );
}

describe('HourlyGridDialog', () => {
  it('renders dialog content with patient name in sr-only title', () => {
    renderDialog('OPEN');
    expect(screen.getByText('вміст карти')).toBeInTheDocument();
    expect(screen.getByText('Погодинна карта — Тестовий Пацієнт')).toBeInTheDocument();
  });

  it.each([
    ['OPEN', 'Відкрито', 'text-warning', 'text-success'],
    ['NURSE_SIGNED', 'Підписано м/с', 'text-info', 'text-warning'],
    ['DOCTOR_SIGNED', 'Підписано лікарем', 'text-success', 'text-warning'],
    ['CLOSED', 'Закрито', 'text-muted-foreground', 'text-warning'],
    ['REOPENED', 'Перевідкрито', 'text-warning', 'text-success'],
  ])(
    'status %s badge is visually distinct (has %s, lacks %s)',
    (status, label, expected, forbidden) => {
      renderDialog(status);
      const badge = screen.getByText(label);
      expect(badge.className).toContain(expected);
      expect(badge.className).not.toContain(forbidden);
    }
  );
});
