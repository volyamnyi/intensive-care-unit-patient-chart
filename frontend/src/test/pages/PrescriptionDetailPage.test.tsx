import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';

const mockUseParams = vi.fn();
const mockGetById = vi.fn();
const mockGetItems = vi.fn();
const mockGetGrid = vi.fn().mockResolvedValue({ data: [] });
const mockAddItemDay = vi.fn();
const mockRemoveItemDay = vi.fn();
const mockCancelMedication = vi.fn();
const mockRestoreToPlanned = vi.fn();
const mockCancelAssignment = vi.fn();
const mockGetPdfZip = vi.fn();
const mockGetPdfInfo = vi.fn();
const mockGetPdfPage = vi.fn();
const mockPrintPdfBlob = vi.fn();
const mockToastSuccess = vi.fn();
const mockToastError = vi.fn();
let mockAuth: () => unknown = () => doctorAuth;

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useParams: () => mockUseParams() };
});

vi.mock('../../api/medication', () => ({
  prescriptionApi: {
    getById: (...a: unknown[]) => mockGetById(...a),
    getItems: (...a: unknown[]) => mockGetItems(...a),
    addItemDay: (...a: unknown[]) => mockAddItemDay(...a),
    removeItemDay: (...a: unknown[]) => mockRemoveItemDay(...a),
    planDose: vi.fn(), completeDose: vi.fn(),
    cancelMedication: (...a: unknown[]) => mockCancelMedication(...a),
    restoreToPlanned: (...a: unknown[]) => mockRestoreToPlanned(...a),
    cancelAssignment: (...a: unknown[]) => mockCancelAssignment(...a),
    executeDose: vi.fn(),
    addItem: vi.fn(), removeItem: vi.fn(), create: vi.fn(), delete: vi.fn(), close: vi.fn(),
    getByPatient: vi.fn(),
    getMedicineCatalog: () => Promise.resolve({ data: [] }),
    getPdfZip: (...a: unknown[]) => mockGetPdfZip(...a),
    getPdfInfo: (...a: unknown[]) => mockGetPdfInfo(...a),
    getPdfPage: (...a: unknown[]) => mockGetPdfPage(...a),
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

vi.mock('../../lib/printPdf', () => ({
  printPdfBlob: (...a: unknown[]) => mockPrintPdfBlob(...a),
}));

vi.mock('sonner', () => ({
  toast: {
    success: (...a: unknown[]) => mockToastSuccess(...a),
    error: (...a: unknown[]) => mockToastError(...a),
    info: vi.fn(),
    warning: vi.fn(),
  },
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

function makeTwoDayItem() {
  const today = new Date().toISOString().slice(0, 10);
  const part = (id: string, dayId: string, dayDate: string) => ({
    id, dayId, dayDate, period: 'morning', dose: '5mg',
    isPlanned: false, isPlannedFinished: false, isCompleted: false, isCompletedFinished: false,
    doctorName: null, nurseName: null,
  });
  return { ...makeItem(), dayParts: [part('dp-1', 'day-1', today), part('dp-2', 'day-2', '2999-12-31')] };
}

function makeCellItem(flags: { isPlanned: boolean; isPlannedFinished: boolean; isCompleted?: boolean }) {
  const today = new Date().toISOString().slice(0, 10);
  return {
    ...makeItem(),
    dayParts: [
      {
        id: 'dp-1', dayId: 'day-1', dayDate: today, period: 'morning', dose: '5mg',
        isPlanned: flags.isPlanned,
        isPlannedFinished: flags.isPlannedFinished,
        isCompleted: flags.isCompleted ?? false,
        isCompletedFinished: false,
        doctorName: null, nurseName: null,
      },
    ],
  };
}
function renderPage(auth: () => unknown = () => doctorAuth, items = [makeItem()]) {
  mockAuth = auth;
  mockUseParams.mockReturnValue({ id: 'list-1' });
  mockGetById.mockResolvedValue({ data: makeList() });
  mockGetItems.mockResolvedValue({ data: items });
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
    mockCancelMedication.mockResolvedValue({ data: makeItem() });
    mockRestoreToPlanned.mockResolvedValue({ data: makeItem() });
    mockCancelAssignment.mockResolvedValue({ data: makeItem() });
  });

  it('doctor: «+ День» adds a day via API and refreshes items', async () => {
    renderPage();
    const btn = await screen.findByRole('button', { name: 'Додати день' });
    await userEvent.click(btn);

    await waitFor(() => expect(mockAddItemDay).toHaveBeenCalledTimes(1));
    expect(mockAddItemDay).toHaveBeenCalledWith('item-1');
    await waitFor(() => expect(mockGetItems).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalledWith('День додано'));
    expect(screen.queryByText(/Не вдалося додати день/)).not.toBeInTheDocument();
  });

  it('doctor: «−» removes the last day via API and refreshes items', async () => {
    renderPage(() => doctorAuth, [makeTwoDayItem()]);
    await userEvent.click(await screen.findByRole('button', { name: 'Видалити день' }));

    await waitFor(() => expect(mockRemoveItemDay).toHaveBeenCalledTimes(1));
    expect(mockRemoveItemDay).toHaveBeenCalledWith('item-1', 'day-2');
    await waitFor(() => expect(mockGetItems).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalledWith('День видалено'));
    expect(screen.queryByText(/Не вдалося видалити день/)).not.toBeInTheDocument();
  });

  it('doctor: failed removeDay shows an error toast with the backend message', async () => {
    const ukMessage = 'День містить виконані призначення, видалення неможливе';
    mockRemoveItemDay.mockRejectedValue({ response: { data: { message: ukMessage }, status: 422 } });
    renderPage(() => doctorAuth, [makeTwoDayItem()]);
    await userEvent.click(await screen.findByRole('button', { name: 'Видалити день' }));

    await waitFor(() => expect(mockRemoveItemDay).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockToastError).toHaveBeenCalledWith(ukMessage));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('doctor: «Відмінити це призначення» calls cancelAssignment and refreshes items', async () => {
    renderPage(() => doctorAuth, [makeCellItem({ isPlanned: true, isPlannedFinished: false })]);

    const row = (await screen.findByText('Dopamine')).closest('tr');
    expect(row).not.toBeNull();
    const cell = row!.querySelectorAll('td')[1];
    fireEvent.contextMenu(cell);

    await userEvent.click(await screen.findByRole('menuitem', { name: /Відмінити це призначення/ }));

    await waitFor(() => expect(mockCancelAssignment).toHaveBeenCalledTimes(1));
    expect(mockCancelAssignment).toHaveBeenCalledWith('dp-1');
    await waitFor(() => expect(mockGetItems).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalledWith('Призначення відмінено'));
  });

  it('doctor: failed cancelAssignment shows an error toast with the backend message', async () => {
    const ukMessage = 'Виконане призначення не може бути відмінене';
    mockCancelAssignment.mockRejectedValue({ response: { data: { message: ukMessage }, status: 422 } });
    renderPage(() => doctorAuth, [makeCellItem({ isPlanned: true, isPlannedFinished: false })]);

    const row = (await screen.findByText('Dopamine')).closest('tr');
    expect(row).not.toBeNull();
    const cell = row!.querySelectorAll('td')[1];
    fireEvent.contextMenu(cell);

    await userEvent.click(await screen.findByRole('menuitem', { name: /Відмінити це призначення/ }));

    await waitFor(() => expect(mockCancelAssignment).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(mockToastError).toHaveBeenCalledWith(ukMessage));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('doctor: «Відмінити препарат» calls cancelMedication and refreshes items', async () => {
    renderPage(() => doctorAuth, [makeCellItem({ isPlanned: true, isPlannedFinished: false })]);

    const row = (await screen.findByText('Dopamine')).closest('tr');
    expect(row).not.toBeNull();
    const cell = row!.querySelectorAll('td')[1];
    fireEvent.contextMenu(cell);

    await userEvent.click(await screen.findByRole('menuitem', { name: /Відмінити препарат/ }));

    await waitFor(() => expect(mockCancelMedication).toHaveBeenCalledTimes(1));
    expect(mockCancelMedication).toHaveBeenCalledWith('dp-1');
    await waitFor(() => expect(mockGetItems).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalledWith('Препарат відмінено'));
  });

  it('doctor: «Повернути у Заплановано» calls restoreToPlanned and refreshes items', async () => {
    renderPage(() => doctorAuth, [makeCellItem({ isPlanned: true, isPlannedFinished: true })]);

    const row = (await screen.findByText('Dopamine')).closest('tr');
    expect(row).not.toBeNull();
    const cell = row!.querySelectorAll('td')[1];
    fireEvent.contextMenu(cell);

    await userEvent.click(await screen.findByRole('menuitem', { name: /Повернути у Заплановано/ }));

    await waitFor(() => expect(mockRestoreToPlanned).toHaveBeenCalledTimes(1));
    expect(mockRestoreToPlanned).toHaveBeenCalledWith('dp-1');
    await waitFor(() => expect(mockGetItems).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalledWith('Повернуто у Заплановано'));
  });

  it('doctor: failed restore shows an error toast with the backend message', async () => {
    const ukMessage = 'Призначення не у статусі «Відмінено», повернення неможливе';
    mockRestoreToPlanned.mockRejectedValue({ response: { data: { message: ukMessage }, status: 422 } });
    renderPage(() => doctorAuth, [makeCellItem({ isPlanned: true, isPlannedFinished: true })]);

    const row = (await screen.findByText('Dopamine')).closest('tr');
    expect(row).not.toBeNull();
    const cell = row!.querySelectorAll('td')[1];
    fireEvent.contextMenu(cell);

    await userEvent.click(await screen.findByRole('menuitem', { name: /Повернути у Заплановано/ }));

    await waitFor(() => expect(mockToastError).toHaveBeenCalledTimes(1));
    expect(mockToastError).toHaveBeenCalledWith(ukMessage);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('nurse: «+ День» is not rendered and API is never called', async () => {
    renderPage(() => nurseAuth);
    await screen.findByText('Dopamine');
    expect(screen.queryByRole('button', { name: 'Додати день' })).not.toBeInTheDocument();
    expect(mockAddItemDay).not.toHaveBeenCalled();
  });
});

describe('PrescriptionDetailPage — Form 003-4/о PDF actions', () => {
  beforeEach(() => {
    mockGetPdfZip.mockReset().mockResolvedValue({ data: new Blob(['PK'], { type: 'application/zip' }) });
    mockGetPdfInfo.mockReset().mockResolvedValue({ data: { pages: 2, fileName: 'prescription-list-1.zip' } });
    mockGetPdfPage.mockReset().mockResolvedValue({ data: new Blob(['%PDF'], { type: 'application/pdf' }) });
    mockPrintPdfBlob.mockReset().mockResolvedValue(undefined);
    mockToastSuccess.mockClear();
    mockToastError.mockClear();
  });

  it('«Завантажити PDF» downloads the backend ZIP batch', async () => {
    const createObjectURL = vi.fn(() => 'blob:mock-zip');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL });
    renderPage(() => doctorAuth);

    await userEvent.click(await screen.findByRole('button', { name: /Завантажити PDF/ }));

    await waitFor(() => expect(mockGetPdfZip).toHaveBeenCalledWith('list-1'));
    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock-zip');
    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalledWith('PDF завантажено'));
    vi.unstubAllGlobals();
  });

  it('«Друкувати PDF» prints every backend sheet from PDF bytes', async () => {
    renderPage(() => doctorAuth);

    await userEvent.click(await screen.findByRole('button', { name: /Друкувати PDF/ }));

    await waitFor(() => expect(mockGetPdfInfo).toHaveBeenCalledWith('list-1'));
    await waitFor(() => expect(mockGetPdfPage).toHaveBeenCalledTimes(2));
    expect(mockGetPdfPage).toHaveBeenNthCalledWith(1, 'list-1', 0);
    expect(mockGetPdfPage).toHaveBeenNthCalledWith(2, 'list-1', 1);
    expect(mockPrintPdfBlob).toHaveBeenCalledTimes(2);
    await waitFor(() => expect(mockToastSuccess).toHaveBeenCalledWith('PDF надіслано на друк (2 стор.)'));
  });

  it('PDF buttons are visible for the nurse view too', async () => {
    renderPage(() => nurseAuth);
    await screen.findByText('Dopamine');
    expect(screen.getByRole('button', { name: /Завантажити PDF/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Друкувати PDF/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Закрити листок/ })).not.toBeInTheDocument();
  });
});
