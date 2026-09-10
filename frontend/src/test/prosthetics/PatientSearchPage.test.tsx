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

const patientA: ProstheticsPatient = { id: 'p1', pib: 'Іван Іванов', birthDate: '1990-01-01', gender: 'Чоловіча' };
const patientB: ProstheticsPatient = { id: 'p2', pib: 'Олена Коваленко', birthDate: '1985-11-22', gender: 'Жіноча' };
// Live-MIS shape (spiPatientProsthesCheck): a registry patient carries no
// local orders and no 120/121 documents — step 1 must still list them.
const patientMis: ProstheticsPatient = { id: '13372', pib: 'Сидоренко Василь Тестович', birthDate: '1962-07-08T00:00:00', gender: 'Чоловіча' };

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

  it('loads the MIS registry on mount and shows patients in a table', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [patientA, patientB] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Іван Іванов')).toBeInTheDocument();
    });
    expect(screen.getByText('Олена Коваленко')).toBeInTheDocument();
    expect(prostheticsPatientApiMock.search).toHaveBeenCalledTimes(1);
  });

  it('lists MIS patients that hold no local orders or MIS documents', async () => {
    // Regression: the candidates worklist gates on local orders + 120/121
    // documents and returns empty for a live MIS roster — step 1 must use
    // the plain registry instead, so these rows stay visible.
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [patientMis, patientA] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Сидоренко Василь Тестович')).toBeInTheDocument();
    });
    expect(screen.getByText('Іван Іванов')).toBeInTheDocument();
  });

  it('keeps the full list visible when the query is shorter than 2 characters', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [patientA] });
    renderPage();
    await waitFor(() => expect(screen.getByText('Іван Іванов')).toBeInTheDocument());
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'i' } });
    await waitFor(() => expect(screen.getByText('Іван Іванов')).toBeInTheDocument());
  });

  it('refines the list via the debounced registry search', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [patientA] });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'Іван' } });
    await waitFor(() => {
      expect(prostheticsPatientApiMock.search).toHaveBeenCalledWith('Іван', expect.any(AbortSignal));
    });
  });

  it('shows empty state when no registry patients match the search', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [] });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'nobody' } });
    await waitFor(() => {
      expect(screen.getByText(/Пацієнтів не знайдено/)).toBeInTheDocument();
    });
  });

  it('renders patient table with results', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [patientA] });
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
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [patientA] });
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
