import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import PrescriptionPage from '../../pages/prescription/PrescriptionPage';

const mockNavigate = vi.fn();
const mockSearch = vi.fn();
const mockGetByPatient = vi.fn();
const mockCreate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../api/platform', () => ({
  patientApi: {
    search: (...args: unknown[]) => mockSearch(...args),
    getById: vi.fn(),
  },
}));

vi.mock('../../api/medication', () => ({
  prescriptionApi: {
    getByPatient: (...args: unknown[]) => mockGetByPatient(...args),
    create: (...args: unknown[]) => mockCreate(...args),
  },
}));

vi.mock('../../services/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 0, login: 'doctor1', fullName: 'Доктор', role: 'DOCTOR', email: 'd@test.com', app: null },
    token: 'mock-token',
    isAuthenticated: true,
    logout: vi.fn(),
    hasRole: (...roles: string[]) => roles.includes('DOCTOR'),
    hasPermission: () => true,
    permissions: ['PRESCRIPTION_CREATE'],
  }),
}));

function makePatient(over: Record<string, unknown> = {}) {
  return {
    id: 1001,
    fullName: 'Петренко Іван',
    birthDate: '1960-01-01',
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
    departmentId: 2,
    room: '1',
    bed: '2',
    doctorName: '',
    ...over,
  };
}

function makeList(id: string) {
  return {
    id,
    patientId: 1001,
    hospitalizationId: null,
    departmentId: null,
    documentName: 'Листок',
    status: 'Active' as const,
    editingUserId: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };
}

function renderPage() {
  return render(
    <ThemeModeProvider>
      <PrescriptionPage />
    </ThemeModeProvider>
  );
}

describe('PrescriptionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // surgery department (id 2) is the default; both patients are in surgery
    mockSearch.mockResolvedValue({ data: [makePatient({ id: 1001 }), makePatient({ id: 1002, fullName: 'Коваленко Олена' })] });
  });

  it('shows Відкрити for every patient, creation lives inside the drawer', async () => {
    // 1001 has no list, 1002 has one
    mockGetByPatient.mockImplementation((patientId: number) =>
      Promise.resolve({ data: patientId === 1002 ? [makeList('L-1002')] : [] })
    );

    renderPage();

    expect(await screen.findByText('Петренко Іван')).toBeInTheDocument();
    expect(screen.getByText('Коваленко Олена')).toBeInTheDocument();

    // the Дії column offers only «Відкрити» for every patient
    expect(screen.getAllByRole('button', { name: /Відкрити/ })).toHaveLength(2);
    expect(screen.queryByRole('button', { name: /Створити/ })).not.toBeInTheDocument();
  });

  it('creates a list from the drawer and navigates to the detail page', async () => {
    mockGetByPatient.mockImplementation((patientId: number) =>
      Promise.resolve({ data: patientId === 1001 ? [makeList('L-1001')] : [] })
    );
    mockCreate.mockResolvedValue({ data: makeList('NEW-1') });

    renderPage();

    // open the drawer for Коваленко (1002) via her row's «Відкрити» button
    const kovalenkoCell = await screen.findByText('Коваленко Олена');
    const row = kovalenkoCell.closest('tr');
    expect(row).not.toBeNull();
    const openBtn = row!.querySelector('button');
    expect(openBtn).not.toBeNull();
    await userEvent.click(openBtn!);

    // the drawer offers «Створити листок» at the top of the lists section
    const createBtn = await screen.findByRole('button', { name: /Створити листок/ });
    await userEvent.click(createBtn);

    await waitFor(() => {
      expect(mockCreate).toHaveBeenCalledTimes(1);
      expect(mockCreate).toHaveBeenCalledWith({ patientId: '1002' });
    });
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/prescriptions/doctor/NEW-1');
    });
  });

  it('shows an error message when creation fails', async () => {
    mockGetByPatient.mockResolvedValue({ data: [] });
    mockCreate.mockRejectedValue({ response: { data: { message: 'Створення заборонено' } } });

    renderPage();

    // open the drawer for the first patient, then create from inside it
    const openButtons = await screen.findAllByRole('button', { name: /Відкрити/ });
    await userEvent.click(openButtons[0]);

    const createBtn = await screen.findByRole('button', { name: /Створити листок/ });
    await userEvent.click(createBtn);

    await waitFor(() => {
      expect(screen.getByText('Створення заборонено')).toBeInTheDocument();
    });
  });
});
