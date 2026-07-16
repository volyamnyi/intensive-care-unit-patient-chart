import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material';
import CreateCardPage from '../../pages/doctor/CreateCardPage';
import type { PatientDto } from '../../types';

const theme = createTheme({});

const mockNavigate = vi.fn();
const mockCreate = vi.fn();
const mockPatientSearch = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../api/endpoints', () => ({
  episodeApi: { create: (...args: unknown[]) => mockCreate(...args) },
}));

vi.mock('../../components/common/PatientSearch', () => ({
  default: ({ onSelect }: { onSelect: (p: PatientDto) => void }) => {
    mockPatientSearch(onSelect);
    return <div data-testid="patient-search">PatientSearch</div>;
  },
}));

const testPatient: PatientDto = {
  id: 1001,
  fullName: 'Петренко Іван',
  birthDate: '1990-05-15',
  sexCode: 'M',
  address: 'м. Київ, вул. Хрещатик',
  phone: '+380501234567',
  email: '',
  externalId1: '123456',
  externalId2: '',
  height: 180,
  weight: 80,
  bloodGroup: 'A',
  rhFactor: '+',
};

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter>
        <CreateCardPage />
      </MemoryRouter>
    </ThemeProvider>
  );
}

describe('CreateCardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the page title', () => {
    renderPage();
    expect(screen.getByText('Нова карта інтенсивної терапії')).toBeInTheDocument();
  });

  it('sets document title on mount', () => {
    renderPage();
    expect(document.title).toBe('ВАІТ — Нова карта');
  });

  it('renders PatientSearch component', () => {
    renderPage();
    expect(screen.getByTestId('patient-search')).toBeInTheDocument();
  });

  it('shows patient data fields when a patient is selected', async () => {
    renderPage();
    const onSelect = mockPatientSearch.mock.calls[0][0];
    onSelect(testPatient);
    await waitFor(() => {
      expect(screen.getByLabelText('ПІП')).toBeInTheDocument();
      expect(screen.getByLabelText('Дата народження')).toBeInTheDocument();
      expect(screen.getByLabelText('Стать')).toBeInTheDocument();
      expect(screen.getByLabelText('Зріст (см)')).toBeInTheDocument();
      expect(screen.getByLabelText('Маса (кг)')).toBeInTheDocument();
      expect(screen.getByLabelText('Група крові')).toBeInTheDocument();
      expect(screen.getByLabelText('Rezus')).toBeInTheDocument();
      expect(screen.getByLabelText('№ медкарти')).toBeInTheDocument();
    });
  });

  it('shows patient data values when patient is selected', async () => {
    renderPage();
    const onSelect = mockPatientSearch.mock.calls[0][0];
    onSelect(testPatient);
    await waitFor(() => {
      expect(screen.getByDisplayValue('Петренко Іван')).toBeInTheDocument();
      expect(screen.getByDisplayValue('1990-05-15')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Чол')).toBeInTheDocument();
      expect(screen.getByDisplayValue('180')).toBeInTheDocument();
      expect(screen.getByDisplayValue('80')).toBeInTheDocument();
      expect(screen.getByDisplayValue('A')).toBeInTheDocument();
      expect(screen.getByDisplayValue('+')).toBeInTheDocument();
      expect(screen.getByDisplayValue('123456')).toBeInTheDocument();
    });
  });

  it('shows create and cancel buttons when patient is selected', async () => {
    renderPage();
    const onSelect = mockPatientSearch.mock.calls[0][0];
    onSelect(testPatient);
    await waitFor(() => {
      expect(screen.getByText('Створити карту')).toBeInTheDocument();
      expect(screen.getByText('Скасувати')).toBeInTheDocument();
    });
  });

  it('calls episodeApi.create and navigates on create', async () => {
    mockCreate.mockResolvedValue({ data: { id: 'ep-1' } });
    renderPage();
    const onSelect = mockPatientSearch.mock.calls[0][0];
    onSelect(testPatient);
    await waitFor(() => expect(screen.getByText('Створити карту')).toBeInTheDocument());
    await userEvent.click(screen.getByText('Створити карту'));
    await waitFor(() => {
      expect(mockCreate).toHaveBeenCalledWith({
        patientId: 1001,
        admissionDate: expect.any(String),
      });
      expect(mockNavigate).toHaveBeenCalledWith('/doctor/episode/ep-1');
    });
  });

  it('shows error alert on create failure', async () => {
    mockCreate.mockRejectedValue(new Error('fail'));
    renderPage();
    const onSelect = mockPatientSearch.mock.calls[0][0];
    onSelect(testPatient);
    await waitFor(() => expect(screen.getByText('Створити карту')).toBeInTheDocument());
    await userEvent.click(screen.getByText('Створити карту'));
    await waitFor(() => {
      expect(screen.getByText('Помилка створення карти')).toBeInTheDocument();
    });
  });

  it('navigates to /doctor on cancel', async () => {
    renderPage();
    const onSelect = mockPatientSearch.mock.calls[0][0];
    onSelect(testPatient);
    await waitFor(() => expect(screen.getByText('Скасувати')).toBeInTheDocument());
    await userEvent.click(screen.getByText('Скасувати'));
    expect(mockNavigate).toHaveBeenCalledWith('/doctor');
  });
});
