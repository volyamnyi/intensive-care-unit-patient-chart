import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import PatientSearch from '../../components/common/PatientSearch';
import type { PatientDto } from '../../types';

const theme = createTheme({});

const mockSearch = vi.fn();
const onSelect = vi.fn();

vi.mock('../../api/endpoints', () => ({
  patientApi: { search: (...args: unknown[]) => mockSearch(...args) },
}));

const testPatients: PatientDto[] = [
  {
    id: 1001, fullName: 'Петренко Іван', birthDate: '1990-05-15',
    sexCode: 'M', address: 'м. Київ, вул. Хрещатик, 1',
    phone: '+380501234567', email: '', externalId1: '123456',
    externalId2: '', height: 180, weight: 80, bloodGroup: 'A', rhFactor: '+',
  },
  {
    id: 1002, fullName: 'Коваленко Олена', birthDate: '1985-10-20',
    sexCode: 'F', address: 'м. Львів, вул. Лесі Українки',
    phone: '+380507654321', email: '', externalId1: '789012',
    externalId2: '', height: 165, weight: null, bloodGroup: 'B', rhFactor: '-',
  },
];

function renderSearch() {
  return render(
    <ThemeProvider theme={theme}>
      <PatientSearch onSelect={onSelect} />
    </ThemeProvider>
  );
}

describe('PatientSearch', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the search input', () => {
    renderSearch();
    expect(screen.getByLabelText('Пошук пацієнта (ПІБ, № медкарти)')).toBeInTheDocument();
  });

  it('shows hint when input is less than 2 characters', async () => {
    renderSearch();
    const input = screen.getByLabelText('Пошук пацієнта (ПІБ, № медкарти)');
    await userEvent.type(input, 'A');
    await waitFor(() => {
      expect(screen.getByText('Введіть мінімум 2 символи')).toBeInTheDocument();
    });
  });

  it('shows no patients found when search has no results', async () => {
    mockSearch.mockResolvedValue({ data: [] });
    renderSearch();
    const input = screen.getByLabelText('Пошук пацієнта (ПІБ, № медкарти)');
    await userEvent.type(input, 'NonExistent');
    await waitFor(() => {
      expect(screen.getByText('Пацієнтів не знайдено')).toBeInTheDocument();
    });
  });

  it('calls patientApi.search with query after debounce', async () => {
    mockSearch.mockResolvedValue({ data: [] });
    renderSearch();
    const input = screen.getByLabelText('Пошук пацієнта (ПІБ, № медкарти)');
    await userEvent.type(input, 'Петр');
    await waitFor(() => {
      expect(mockSearch).toHaveBeenCalledWith('Петр', expect.any(Object));
    });
  });

  it('displays patient options in dropdown', async () => {
    mockSearch.mockResolvedValue({ data: testPatients });
    renderSearch();
    const input = screen.getByLabelText('Пошук пацієнта (ПІБ, № медкарти)');
    await userEvent.type(input, 'Петр');
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
      expect(screen.getByText('Коваленко Олена')).toBeInTheDocument();
    });
  });

  it('calls onSelect when a patient is chosen', async () => {
    mockSearch.mockResolvedValue({ data: testPatients });
    renderSearch();
    const input = screen.getByLabelText('Пошук пацієнта (ПІБ, № медкарти)');
    await userEvent.type(input, 'Петр');
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
    });
    const option = screen.getByText('Петренко Іван').closest('li')!;
    await userEvent.click(option);
    await waitFor(() => {
      expect(onSelect).toHaveBeenCalledWith(testPatients[0]);
    });
  });

  it('shows loading indicator during search', async () => {
    mockSearch.mockReturnValue(new Promise(() => {}));
    renderSearch();
    const input = screen.getByLabelText('Пошук пацієнта (ПІБ, № медкарти)');
    await userEvent.type(input, 'Петр');
    await waitFor(() => {
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });
  });

  it('renders with custom label', () => {
    render(
      <ThemeProvider theme={theme}>
        <PatientSearch onSelect={onSelect} label="Знайти пацієнта" />
      </ThemeProvider>
    );
    expect(screen.getByLabelText('Знайти пацієнта')).toBeInTheDocument();
  });
});
