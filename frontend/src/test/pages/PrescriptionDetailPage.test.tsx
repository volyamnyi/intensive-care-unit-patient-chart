import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';

const mockUseParams = vi.fn();
const mockGetById = vi.fn();
const mockGetItems = vi.fn();
const mockGetAllergies = vi.fn().mockResolvedValue({ data: [] });
const mockGetGrid = vi.fn().mockResolvedValue({ data: [] });
const mockAddItemDay = vi.fn();
const mockRemoveItemDay = vi.fn();
let mockAuth: () => unknown = () => doctorAuth;

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useParams: () => mockUseParams() };
});

vi.mock('../../api/medication', () => ({
  prescriptionApi: {
    getById: (...a: unknown[]) => mockGetById(...a),
    getItems: (...a: unknown[]) => mockGetItems(...a),
    getAllergies: (...a: unknown[]) => mockGetAllergies(...a),
    addItemDay: (...a: unknown[]) => mockAddItemDay(...a),
    removeItemDay: (...a: unknown[]) => mockRemoveItemDay(...a),
    planDose: vi.fn(), completeDose: vi.fn(), cancelDose: vi.fn(), executeDose: vi.fn(),
    addItem: vi.fn(), removeItem: vi.fn(), create: vi.fn(), delete: vi.fn(), close: vi.fn(),
    getByPatient: vi.fn(),
    getMedicineCatalog: () => Promise.resolve({ data: [] }),
  },
  vitalSignApi: {
    getGrid: (...a: unknown[]) => mockGetGrid(...a),
    getByPrescriptionList: vi.fn(), getEntries: vi.fn(), create: vi.fn(),
    updateEntry: vi.fn(), updateCell: vi.fn(),
  },
}));

const doctorAuth = {
  user: { id: 1, login: 'doctor1', fullName: 'Доктор', role: 'DOCTOR', email: 'd@test.com', app: null },
  token: 'mock-token',
  isAuthenticated: true,
  logout: vi.fn(),
  hasRole: (...roles: string[]) => roles.includes('DOCTOR'),
  hasPermission: () => true,
  permissions: [],
};

const nurseAuth = { ...doctorAuth, user: { ...doctorAuth.user, id: 2, login: 'nurse1', role: 'NURSE' } };

vi.mock('../../services/AuthContext', () => ({
  useAuth: () => mockAuth(),
}));

import PrescriptionDetailPage from '../../pages/prescription/PrescriptionDetailPage';

function makeList() {
  return {
    id: 'list-1',
    patientId: 1001,
    hospitalizationId: null,
    departmentId: null,
    documentName: 'Листок призначень',
    status: 'Active' as const,
    editingUserId: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };
}

function makeItem(id = 'item-1', dayId = 'day-1') {
  const today = new Date().toISOString().slice(0, 10);
  return {
    id,
    listId: 'list-1',
    medicineName: 'Dopamine',
    medicineMethod: 'IV',
    regime: 'stat',
    status: 'Active' as const,
    sortOrder: 0,
    dayParts: [
      {
        id: 'dp-1', dayId, dayDate: today, period: 'morning', dose: '5mg',
        isPlanned: false, isPlannedFinished: false, isCompleted: false, isCompletedFinished: false,
        doctorName: null, nurseName: null,
      },
    ],
  };
}

function renderPage(auth: () => unknown = () => doctorAuth) {
  mockAuth = auth;
  mockUseParams.mockReturnValue({ id: 'list-1' });
  mockGetById.mockResolvedValue({ data: makeList() });
  mockGetItems.mockResolvedValue({ data: [makeItem()] });
  return render(
    <ThemeModeProvider>
      <PrescriptionDetailPage />
    </ThemeModeProvider>
  );
}

describe('PrescriptionDetailPage — per-item day actions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuth = () => doctorAuth;
    mockAddItemDay.mockResolvedValue({ data: makeItem() });
    mockRemoveItemDay.mockResolvedValue({ data: null });
  });

  it('doctor: «+ День» adds a day via API and refreshes items', async () => {
    renderPage();
    const btn = await screen.findByRole('button', { name: 'Додати день' });
    await userEvent.click(btn);

    await waitFor(() => expect(mockAddItemDay).toHaveBeenCalledTimes(1));
    expect(mockAddItemDay).toHaveBeenCalledWith('item-1');
    await waitFor(() => expect(mockGetItems).toHaveBeenCalledTimes(2));
    expect(screen.queryByText(/Не вдалося додати день/)).not.toBeInTheDocument();
  });

  it('doctor: failed removeDay surfaces the backend message', async () => {
    const ukMessage = 'День містить виконані призначення, видалення неможливе';
    mockRemoveItemDay.mockRejectedValue({ response: { data: { message: ukMessage }, status: 422 } });
    renderPage();

    const row = (await screen.findByText('Dopamine')).closest('tr');
    expect(row).not.toBeNull();
    const cell = row!.querySelectorAll('td')[1];
    fireEvent.contextMenu(cell);

    await userEvent.click(await screen.findByRole('menuitem', { name: /Видалити цей день/ }));

    await waitFor(() => expect(mockRemoveItemDay).toHaveBeenCalledTimes(1));
    expect(mockRemoveItemDay).toHaveBeenCalledWith('item-1', 'day-1');
    await waitFor(() => expect(screen.getByText(ukMessage)).toBeInTheDocument());
    expect(screen.queryByText('Не вдалося видалити день')).not.toBeInTheDocument();
  });

  it('nurse: «+ День» is not rendered and API is never called', async () => {
    renderPage(() => nurseAuth);
    await screen.findByText('Dopamine');
    expect(screen.queryByRole('button', { name: 'Додати день' })).not.toBeInTheDocument();
    expect(mockAddItemDay).not.toHaveBeenCalled();
  });
});
