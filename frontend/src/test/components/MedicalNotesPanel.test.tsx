import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import MedicalNotesPanel from '../../components/common/MedicalNotesPanel';
import type { MedicalNote } from '../../types';

const mockNote: MedicalNote = {
  id: 'note-1',
  clinicalDayId: 'day-1',
  authorId: 1,
  role: 'DOCTOR',
  noteType: 'DOCTOR_NOTE',
  text: 'Стан стабільний, динаміка позитивна.',
  createdAt: '2025-06-01T12:00:00Z',
  updatedAt: '2025-06-01T12:00:00Z',
  version: 1,
};

function renderPanel(props: Partial<React.ComponentProps<typeof MedicalNotesPanel>> = {}) {
  return render(
    <ThemeModeProvider>
      <MedicalNotesPanel
        notes={props.notes ?? []}
        onCreateNote={props.onCreateNote}
      />
    </ThemeModeProvider>
  );
}

describe('MedicalNotesPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders existing notes with text', () => {
    renderPanel({ notes: [mockNote] });
    expect(screen.getByText('Стан стабільний, динаміка позитивна.')).toBeInTheDocument();
  });

  it('shows "Немає нотаток" empty state', () => {
    renderPanel({ notes: [] });
    expect(screen.getByText('Немає нотаток')).toBeInTheDocument();
  });

  it('renders create note UI when onCreateNote is provided', () => {
    renderPanel({ onCreateNote: vi.fn() });
    expect(screen.getByText('Додати')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Нова нотатка')).toBeInTheDocument();
  });

  it('calls onCreateNote when text is entered and submitted', async () => {
    const onCreateNote = vi.fn();
    renderPanel({ onCreateNote });
    const textarea = screen.getByPlaceholderText('Нова нотатка');
    await userEvent.type(textarea, 'Температура тіла в нормі.');
    await userEvent.click(screen.getByText('Додати'));
    await waitFor(() => {
      expect(onCreateNote).toHaveBeenCalledWith('Температура тіла в нормі.', 'DOCTOR_NOTE');
    });
  });

  it('clears textarea after note creation', async () => {
    const onCreateNote = vi.fn();
    renderPanel({ onCreateNote });
    const textarea = screen.getByPlaceholderText('Нова нотатка') as HTMLTextAreaElement;
    await userEvent.type(textarea, 'Нотатка для очищення');
    await userEvent.click(screen.getByText('Додати'));
    await waitFor(() => {
      expect(onCreateNote).toHaveBeenCalled();
    });
    expect(textarea.value).toBe('');
  });

  it('does not create empty note on submit', async () => {
    const onCreateNote = vi.fn();
    renderPanel({ onCreateNote });
    await userEvent.click(screen.getByText('Додати'));
    expect(onCreateNote).not.toHaveBeenCalled();
  });
});
