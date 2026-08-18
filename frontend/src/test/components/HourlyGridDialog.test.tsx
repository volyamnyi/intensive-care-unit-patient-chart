import { afterEach, describe, it, expect, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { useState, type ComponentProps } from 'react';
import HourlyGridDialog from '../../components/monitoring/HourlyGridDialog';
import type { ClinicalDay, Episode, HourlyRecord } from '../../types/icu';

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

function renderDialog(status: ClinicalDay['status'], props: Partial<ComponentProps<typeof HourlyGridDialog>> = {}) {
  return render(
    <HourlyGridDialog
      open
      onOpenChange={vi.fn()}
      episode={episode}
      selectedDay={{ ...selectedDay, status }}
      isLocked={false}
      saveStatus="saved"
      {...props}
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
    expect(screen.getByRole('button', { name: 'Закрити вікно (Esc)' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Закрити вікно' })).toBeInTheDocument();
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

  it('shows a lock banner between the toolbar and body when the day is signed while open', () => {
    renderDialog('NURSE_SIGNED', { isLocked: true });
    expect(screen.getByText('Доба підписана — перегляд')).toBeInTheDocument();
    expect(screen.getByText('Перегляд підписаної доби')).toBeInTheDocument();
  });

  it('shows a subtle loading spinner in the toolbar while day data is refreshing', () => {
    const { unmount } = renderDialog('OPEN', { loading: true });
    expect(screen.getByRole('img', { name: 'Завантаження' })).toBeInTheDocument();
    unmount();
    renderDialog('OPEN');
    expect(screen.queryByRole('img', { name: 'Завантаження' })).not.toBeInTheDocument();
  });

  it('renders the 409 conflict banner with both resolution actions', () => {
    const onResolveConflict = vi.fn();
    renderDialog('OPEN', {
      conflict: { hour: 8, key: 'heartRate', raw: '99' },
      onResolveConflict,
    });
    expect(screen.getByText('Запис змінено іншим користувачем (heartRate 8:00)')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Оновити дані' }));
    expect(onResolveConflict).toHaveBeenCalledWith(false);
    fireEvent.click(screen.getByRole('button', { name: 'Залишити мій варіант' }));
    expect(onResolveConflict).toHaveBeenCalledWith(true);
  });
});

describe('HourlyGridDialog glance layer (issue #139)', () => {
  const rec = (hour: number, overrides: Partial<HourlyRecord> = {}): HourlyRecord => ({
    id: `r${hour}`,
    clinicalDayId: 'day-1',
    recordTime: `2025-06-01T${String(hour).padStart(2, '0')}:00:00Z`,
    consciousness: null,
    gcs: null,
    temperature: null,
    heartRate: null,
    respiratoryRate: null,
    systolicBP: null,
    diastolicBP: null,
    meanArterialPressure: null,
    spo2: null,
    etco2: null,
    fio2: null,
    cvp: null,
    dopamine: null,
    dobutamine: null,
    norepinephrine: null,
    epinephrine: null,
    urineOutput: null,
    drainOutput: null,
    gastricOutput: null,
    stool: null,
    vomit: null,
    bedPosition: null,
    headEnd: null,
    painScore: null,
    notes: null,
    createdBy: 1,
    createdAt: '',
    updatedBy: 1,
    updatedAt: '',
    version: 1,
    ...overrides,
  });

  const recByHour = (entries: Array<[number, Partial<HourlyRecord>]>): Map<number, HourlyRecord> =>
    new Map(entries.map(([h, o]) => [h, rec(h, o)]));

  it('shows the fill counter in the footer (0/24 without data)', () => {
    renderDialog('OPEN');
    expect(screen.getByText('Заповнено 0/24 год')).toBeInTheDocument();
  });

  it('counts filled hours from recByHour', () => {
    renderDialog('OPEN', { recByHour: recByHour([[8, { heartRate: 80 }], [9, { spo2: 97 }]]) });
    expect(screen.getByText('Заповнено 2/24 год')).toBeInTheDocument();
  });

  it('hides the alarm chip without critical values and shows a pluralized count with them', () => {
    renderDialog('OPEN');
    expect(screen.queryByRole('button', { name: 'Показати перше критичне значення' })).toBeNull();
    renderDialog('OPEN', { recByHour: recByHour([[8, { heartRate: 131 }]]) });
    expect(screen.getByText('1 критичне значення')).toBeInTheDocument();
    renderDialog('OPEN', { recByHour: recByHour([[8, { heartRate: 131 }], [9, { spo2: 89 }]]) });
    expect(screen.getByText('2 критичні значення')).toBeInTheDocument();
    renderDialog('OPEN', { recByHour: recByHour([[1, { heartRate: 131 }], [2, { spo2: 89 }], [3, { heartRate: 150 }], [4, { spo2: 88 }], [5, { heartRate: 131 }]]) });
    expect(screen.getByText('5 критичних значень')).toBeInTheDocument();
  });

  it('alarm chip click scrolls to and focuses the first critical cell in DOM order', () => {
    const scrollIntoView = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoView;
    render(
      <HourlyGridDialog
        open
        onOpenChange={vi.fn()}
        episode={episode}
        selectedDay={selectedDay}
        isLocked={false}
        saveStatus="saved"
        recByHour={recByHour([[8, { heartRate: 131 }], [9, { spo2: 89 }]])}
      >
        <table>
          <tbody>
            <tr><td data-critical="true"><input aria-label="ЧСС 8:00" /></td></tr>
            <tr><td data-critical="true"><input aria-label="SpO₂ 9:00" /></td></tr>
          </tbody>
        </table>
      </HourlyGridDialog>
    );
    fireEvent.click(screen.getByRole('button', { name: 'Показати перше критичне значення' }));
    expect(scrollIntoView).toHaveBeenCalledWith({ block: 'center', inline: 'center' });
    expect(screen.getByLabelText('ЧСС 8:00')).toHaveFocus();
  });

  it('colors the save status with AA-green when saved and destructive on error', () => {
    renderDialog('OPEN', { saveStatus: 'saved' });
    expect(screen.getByText('Збережено').className).toContain('text-[#2E7D32]');
    expect(screen.getByText('Збережено').className).toContain('dark:text-[#81C784]');
    renderDialog('OPEN', { saveStatus: 'error' });
    expect(screen.getByText('Помилка збереження').className).toContain('text-destructive');
    renderDialog('OPEN', { saveStatus: 'saving' });
    expect(screen.getByText('Зберігається…').className).toContain('text-muted-foreground');
  });
});

describe('HourlyGridDialog Phase 8 (issue #141)', () => {
  it('renders no portal content while closed', () => {
    render(
      <HourlyGridDialog
        open={false}
        onOpenChange={vi.fn()}
        episode={episode}
        selectedDay={selectedDay}
        isLocked={false}
        saveStatus="saved"
      >
        <div>{'вміст карти'}</div>
      </HourlyGridDialog>
    );
    expect(document.querySelector('[data-slot="dialog-content"]')).toBeNull();
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('mounts children inside the dialog on open and unmounts them on close', async () => {
    function Controlled() {
      const [open, setOpen] = useState(false);
      return (
        <>
          <button type="button" onClick={() => setOpen(true)}>{'відкрити'}</button>
          <HourlyGridDialog
            open={open}
            onOpenChange={setOpen}
            episode={episode}
            selectedDay={selectedDay}
            isLocked={false}
            saveStatus="saved"
          >
            <div>{'вміст карти'}</div>
          </HourlyGridDialog>
        </>
      );
    }
    render(<Controlled />);
    expect(screen.queryByRole('dialog')).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: 'відкрити' }));
    const dialog = screen.getByRole('dialog');
    expect(dialog).toContainElement(screen.getByText('вміст карти'));

    fireEvent.keyDown(document, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(screen.queryByText('вміст карти')).toBeNull();
  });

  it('disables the modal undo button while the day is locked and enables it when changes exist', () => {
    renderDialog('NURSE_SIGNED', { isLocked: true });
    expect(screen.getByRole('button', { name: 'Скасувати останню зміну' })).toBeDisabled();
    renderDialog('OPEN', { undoCount: 2 });
    expect(screen.getByRole('button', { name: 'Скасувати останню зміну' })).toBeEnabled();
    renderDialog('OPEN');
    expect(screen.getByRole('button', { name: 'Скасувати останню зміну' })).toBeDisabled();
  });
});
