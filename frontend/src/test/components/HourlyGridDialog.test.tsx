import { afterEach, describe, it, expect, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { useState } from 'react';
import HourlyGridDialog from '../../components/monitoring/HourlyGridDialog';
import type { ClinicalDay, Episode } from '../../types';

afterEach(() => {
  document.querySelector('[aria-label="Розгорнути на весь екран"]')?.remove();
});

const selectedDay = {
  id: 'day-1', episodeId: 'ep-1', dayNumber: 1,
  startDateTime: '2025-06-01T08:00:00Z', endDateTime: '2025-06-02T08:00:00Z',
  status: 'OPEN', doctorSigned: false, nurseSigned: false, closedAt: null,
  weightKg: 72, bmi: null, createdBy: 1, createdAt: '',
  updatedBy: 0, updatedAt: '', version: 1,
} as ClinicalDay;

const episode = { id: 'ep-1', patientName: 'Тестовий Пацієнт' } as Episode;

function renderDialog(status: ClinicalDay['status']) {
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

  const badgeRows: Array<[ClinicalDay['status'], string, string, string]> = [
    ['OPEN', 'Відкрито', 'text-warning', 'text-success'],
    ['NURSE_SIGNED', 'Підписано м/с', 'text-info', 'text-warning'],
    ['DOCTOR_SIGNED', 'Підписано лікарем', 'text-success', 'text-warning'],
    ['CLOSED', 'Закрито', 'text-muted-foreground', 'text-warning'],
    ['REOPENED', 'Перевідкрито', 'text-warning', 'text-success'],
  ];
  it.each(badgeRows)(
    'status %s badge is visually distinct (has %s, lacks %s)',
    (status, label, expected, forbidden) => {
      renderDialog(status);
      const badge = screen.getByText(label);
      expect(badge.className).toContain(expected);
      expect(badge.className).not.toContain(forbidden);
    }
  );

  it('marks the dialog popup with data-fullscreen for the CSS morph scoping', () => {
    renderDialog('OPEN');
    expect(document.querySelector('[data-slot="dialog-content"][data-fullscreen="true"]')).not.toBeNull();
  });

  it('sets transformOrigin on the popup from the trigger rect (morph anchor)', () => {
    const trigger = document.createElement('button');
    trigger.setAttribute('aria-label', 'Розгорнути на весь екран');
    document.body.appendChild(trigger);
    renderDialog('OPEN');
    const popup = document.querySelector('[data-slot="dialog-content"]');
    expect(popup).not.toBeNull();
    expect((popup as HTMLElement).style.transformOrigin).toBe('0px 0px');
  });

  it('focuses the header close button on open (data-entry safety, deliberate deviation from APG default)', async () => {
    renderDialog('OPEN');
    const close = screen.getAllByRole('button', { name: 'Закрити вікно (Esc)' })[0];
    await waitFor(() => expect(document.activeElement).toBe(close));
  });

  it('marks the popup as role=dialog with aria-modal=true', () => {
    renderDialog('OPEN');
    const popup = document.querySelector('[data-slot="dialog-content"]');
    expect(popup?.getAttribute('role')).toBe('dialog');
    expect(popup?.getAttribute('aria-modal')).toBe('true');
  });

  it('announces save feedback via a polite live region (SC 3.2.5)', () => {
    render(
      <HourlyGridDialog
        open
        onOpenChange={vi.fn()}
        episode={episode}
        selectedDay={selectedDay}
        isLocked={false}
        saveStatus="saved"
        feedback={{ message: 'Збережено 14:00', severity: 'success' }}
      >
        <div>{'вміст карти'}</div>
      </HourlyGridDialog>
    );
    const status = screen.getByRole('status');
    expect(status).toHaveAttribute('aria-live', 'polite');
    expect(status).toHaveTextContent('Збережено 14:00');
  });

  it('gives close buttons a unique accessible label (no ambiguous "Закрити (Esc)")', () => {
    renderDialog('OPEN');
    expect(screen.getAllByRole('button', { name: 'Закрити вікно (Esc)' }).length).toBeGreaterThanOrEqual(2);
    expect(screen.queryAllByRole('button', { name: 'Закрити (Esc)' })).toHaveLength(0);
  });

  it('returns focus to the trigger and blurs the active element when the dialog closes (WCAG 2.4.3)', async () => {
    const trigger = document.createElement('button');
    trigger.setAttribute('aria-label', 'Розгорнути на весь екран');
    document.body.appendChild(trigger);
    const triggerRef = { current: trigger };

    function Controlled() {
      const [open, setOpen] = useState(true);
      return (
        <HourlyGridDialog
          open={open}
          onOpenChange={setOpen}
          episode={episode}
          selectedDay={selectedDay}
          isLocked={false}
          saveStatus="saved"
          finalFocusRef={triggerRef}
        >
          <div><input data-testid="cell-input" /></div>
        </HourlyGridDialog>
      );
    }

    render(<Controlled />);
    const input = screen.getByTestId('cell-input');
    input.focus();
    fireEvent.keyDown(document, { key: 'Escape' });
    await waitFor(() => expect(document.activeElement).toBe(trigger));
    expect(document.activeElement).not.toBe(input);
  });
});
