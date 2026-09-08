import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import NursePrescriptionPage from '../../pages/prescription/NursePrescriptionPage';

const mockNavigate = vi.fn();
const mockSearchByModule = vi.fn();
const mockGetByPatient = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../api/platform', () => ({
  patientApi: {
    searchByModule: (...args: unknown[]) => mockSearchByModule(...args),
    getById: vi.fn(),
  },
}));

vi.mock('../../api/medication', () => ({
  prescriptionApi: {
    getByPatient: (...args: unknown[]) => mockGetByPatient(...args),
  },
}));

function makePatient(over: Record<string, unknown> = {}) {
  return {
    id: 1003,
    fullName: 'Сидоренко Олег',
    birthDate: '1962-07-08',
    sexCode: 'M',
    address: '',
    phone: '',
    email: '',
    externalId1: '',
    externalId2: '',
    height: null,
    weight: null,
    bloodGroup: '',
    rhFactor: '',
    departmentId: 19,
    room: '503',
    bed: '3',
    doctorName: '',
    ...over,
  };
}

function renderPage() {
  return render(
    <ThemeModeProvider>
      <NursePrescriptionPage />
    </ThemeModeProvider>
  );
}

describe('NursePrescriptionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    mockGetByPatient.mockResolvedValue({ data: [] });
    mockSearchByModule.mockResolvedValue({ data: [makePatient()] });
  });

  it('fetches the medication roster once via searchByModule', async () => {
    renderPage();

    expect(await screen.findByText('Сидоренко Олег')).toBeInTheDocument();
    expect(mockSearchByModule).toHaveBeenCalledTimes(1);
    expect(mockSearchByModule).toHaveBeenCalledWith('medication', '');
  });

  it('toggle splits surgery (19) and rehab (37) and persists the choice', async () => {
    mockSearchByModule.mockResolvedValue({
      data: [
        makePatient({ id: 1003, departmentId: 19 }),
        makePatient({ id: 1002, fullName: 'Коваленко Олена', departmentId: 37 }),
      ],
    });

    renderPage();

    expect(await screen.findByText('Сидоренко Олег')).toBeInTheDocument();
    expect(screen.queryByText('Коваленко Олена')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Реабілітація' }));

    expect(await screen.findByText('Коваленко Олена')).toBeInTheDocument();
    expect(screen.queryByText('Сидоренко Олег')).not.toBeInTheDocument();
    expect(localStorage.getItem('nursePrescDept')).toBe('rehab');
  });

  it('shows an error with retry that refetches the roster', async () => {
    mockSearchByModule
      .mockRejectedValueOnce({ response: { data: { message: 'MIS недоступна' } } })
      .mockResolvedValueOnce({ data: [makePatient()] });

    renderPage();

    expect(await screen.findByText('MIS недоступна')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Спробувати ще/ }));

    expect(await screen.findByText('Сидоренко Олег')).toBeInTheDocument();
    expect(mockSearchByModule).toHaveBeenCalledTimes(2);
  });

  it('shows the empty state when the department has no patients', async () => {
    mockSearchByModule.mockResolvedValue({ data: [] });

    renderPage();

    expect(await screen.findByText('Немає пацієнтів у відділенні')).toBeInTheDocument();
  });
});
