import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PatientSearchPage from '@/pages/prosthetics/setup/PatientSearchPage';
import type { ProstheticsPatient } from '@/prosthetics/types';

const prostheticsPatientApiMock = vi.hoisted(() => ({
  search: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  prostheticsPatientApi: prostheticsPatientApiMock,
}));

const useProsthetics = vi.hoisted(() => vi.fn());

vi.mock('@/prosthetics/ProstheticsContext', () => ({
  useProsthetics,
}));

function mockUseProsthetics(draft = { patientId: null, orderId: null, templateId: null, instanceId: null }) {
  useProsthetics.mockReturnValue({
    draft,
    setDraftField: vi.fn(),
    resetDraft: vi.fn(),
  });
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/prosthetics/new/select-patient']}>
      <PatientSearchPage />
    </MemoryRouter>,
  );
}

describe('PatientSearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [] });
    mockUseProsthetics();
  });

  it('renders the page title and search input', () => {
    renderPage();
    expect(screen.getByText('Вибір пацієнта')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/пошук пацієнта/i)).toBeInTheDocument();
  });

  it('loads all patients on mount and shows them in a table', async () => {
    const patients: ProstheticsPatient[] = [
      { id: 'p1', pib: 'Іван Іванов', birthDate: '1990-01-01', gender: 'Чоловіча' },
      { id: 'p2', pib: 'Олена Коваленко', birthDate: '1985-11-22', gender: 'Жіноча' },
    ];
    prostheticsPatientApiMock.search.mockResolvedValue({ data: patients });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Іван Іванов')).toBeInTheDocument();
    });
    expect(screen.getByText('Олена Коваленко')).toBeInTheDocument();
  });

  it('keeps the full list visible when the query is shorter than 2 characters', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({
      data: [{ id: 'p1', pib: 'Іван Іванов', birthDate: '1990-01-01', gender: 'Чоловіча' }],
    });
    renderPage();
    await waitFor(() => expect(screen.getByText('Іван Іванов')).toBeInTheDocument());
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'i' } });
    await waitFor(() => expect(screen.getByText('Іван Іванов')).toBeInTheDocument());
  });

  it('refines the list via the debounced server search', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({
      data: [{ id: 'p1', pib: 'Іван Іванов', birthDate: '1990-01-01', gender: 'Чоловіча' }],
    });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'Іван' } });
    await waitFor(() => {
      expect(prostheticsPatientApiMock.search).toHaveBeenCalledWith('Іван', expect.any(AbortSignal));
    });
  });

  it('shows empty state when no patients match the search', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [] });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'nobody' } });
    await waitFor(() => {
      expect(screen.getByText(/Пацієнтів не знайдено/)).toBeInTheDocument();
    });
  });

  it('renders patient table with results', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({
      data: [{ id: 'p1', pib: 'Іван Іванов', birthDate: '1990-01-01', gender: 'Чоловіча' }],
    });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'Іван' } });
    await waitFor(() => {
      expect(screen.getByText('Іван Іванов')).toBeInTheDocument();
    });
  });

  it('calls setDraftField and navigates on patient select', async () => {
    const setDraftField = vi.fn();
    useProsthetics.mockReturnValue({
      draft: { patientId: null, orderId: null, templateId: null, instanceId: null },
      setDraftField,
      resetDraft: vi.fn(),
    });
    prostheticsPatientApiMock.search.mockResolvedValue({
      data: [{ id: 'p1', pib: 'Іван Іванов', birthDate: '1990-01-01', gender: 'Чоловіча' }],
    });
    renderPage();
    await waitFor(() => expect(screen.getByText('Іван Іванов')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Обрати/i }));
    expect(setDraftField).toHaveBeenCalledWith('patientId', 'p1');
  });

  it('shows error on search failure', async () => {
    prostheticsPatientApiMock.search.mockRejectedValue(new Error('network'));
    renderPage();
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'ivan' } });
    await waitFor(() => {
      expect(screen.getByText(/Помилка пошуку|Не вдалося завантажити пацієнтів/)).toBeInTheDocument();
    });
  });
});
