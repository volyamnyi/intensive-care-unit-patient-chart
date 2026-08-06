import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PatientSearchPage from '@/pages/prosthetics/setup/PatientSearchPage';

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
  mockUseProsthetics();
  return render(
    <MemoryRouter initialEntries={['/prosthetics/new/select-patient']}>
      <PatientSearchPage />
    </MemoryRouter>,
  );
}

describe('PatientSearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page title and search input', () => {
    renderPage();
    expect(screen.getByText('Вибір пацієнта')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/пошук пацієнта/i)).toBeInTheDocument();
  });

  it('shows prompt when query is shorter than 2 characters', () => {
    renderPage();
      expect(screen.getByText(/Введіть ім'я або номер для пошуку/)).toBeInTheDocument();
  });

  it('clears patients when query drops below 2 chars', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [{ id: 'p1', pib: 'Іван' }] });
    renderPage();
    const input = screen.getByPlaceholderText(/пошук пацієнта/i);
    fireEvent.change(input, { target: { value: 'iv' } });
    await waitFor(() => expect(prostheticsPatientApiMock.search).toHaveBeenCalled());
    fireEvent.change(input, { target: { value: 'i' } });
    await waitFor(() => {
      expect(screen.getByText(/Введіть ім'я або номер для пошуку/)).toBeInTheDocument();
    });
  });

  it('searches patients after debounce', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [{ id: 'p1', pib: 'Іван Іванов' }] });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'Іван' } });
    await waitFor(() => {
      expect(prostheticsPatientApiMock.search).toHaveBeenCalledWith('Іван', expect.any(AbortSignal));
    });
  });

  it('shows empty state when no patients found', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({ data: [] });
    renderPage();
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'nobody' } });
    await waitFor(() => {
      expect(screen.getByText(/Пацієнтів не знайдено/)).toBeInTheDocument();
    });
  });

  it('renders patient table with results', async () => {
    prostheticsPatientApiMock.search.mockResolvedValue({
      data: [{ id: 'p1', pib: 'Іван Іванов', birthDate: '1990-01-01' }],
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
      data: [{ id: 'p1', pib: 'Іван Іванов', birthDate: '1990-01-01' }],
    });
    render(
      <MemoryRouter initialEntries={['/prosthetics/new/select-patient']}>
        <PatientSearchPage />
      </MemoryRouter>,
    );
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'Іван' } });
    await waitFor(() => expect(screen.getByText('Іван Іванов')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Обрати/i }));
    expect(setDraftField).toHaveBeenCalledWith('patientId', 'p1');
  });

  it('shows error on search failure', async () => {
    prostheticsPatientApiMock.search.mockRejectedValue(new Error('network'));
    renderPage();
    fireEvent.change(screen.getByPlaceholderText(/пошук пацієнта/i), { target: { value: 'ivan' } });
    await waitFor(() => {
      expect(screen.getByText(/Помилка пошуку/)).toBeInTheDocument();
    });
  });
});