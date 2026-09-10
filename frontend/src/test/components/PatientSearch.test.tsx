import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import PatientSearch from '../../components/common/PatientSearch';
import type { PatientDto } from '../../types/core';


const mockSearch = vi.fn();
const onSelect = vi.fn();

vi.mock('../../api/platform', () => ({
  patientApi: { searchByModule: (...args: unknown[]) => mockSearch(...args) },
}));

const testPatients: PatientDto[] = [
  {
    id: 13372, fullName: 'Петренко Іван', birthDate: '1990-05-15T00:00:00',
    sexCode: 'MAL', address: 'м. Київ, вул. Хрещатик, 1',
    phone: '+380501234567', email: '', bloodGroup: 'A', rhFactor: 'NEG',
    departmentId: 19, room: '411A-Тестова', bed: 'Ліжко №1', doctorName: 'Ямний В. М.',
  },
  {
    id: 13373, fullName: 'Коваленко Олена', birthDate: '1985-10-20T00:00:00',
    sexCode: 'FEM', address: 'м. Львів, вул. Лесі Українки',
    phone: '+380507654321', email: '', bloodGroup: 'B', rhFactor: 'POS',
    departmentId: 19, room: '611A-Тестова', bed: 'Ліжко №3', doctorName: 'Ямний В. М.',
  },
];

function renderSearch() {
  return render(
    <ThemeModeProvider>
      <PatientSearch onSelect={onSelect} />
    </ThemeModeProvider>
  );
}

describe('PatientSearch', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the search input', () => {
    renderSearch();
    expect(screen.getByLabelText('ПІБ, телефон або ID')).toBeInTheDocument();
  });

  it('shows hint when input is less than 2 characters', async () => {
    renderSearch();
    const input = screen.getByLabelText('ПІБ, телефон або ID');
    await userEvent.type(input, 'A');
    await waitFor(() => {
      expect(screen.getByText('Введіть мінімум 2 символи')).toBeInTheDocument();
    });
  });

  it('shows no patients found when search has no results', async () => {
    mockSearch.mockResolvedValue({ data: [] });
    renderSearch();
    const input = screen.getByLabelText('ПІБ, телефон або ID');
    await userEvent.type(input, 'NonExistent');
    await waitFor(() => {
      expect(screen.getByText('Пацієнтів не знайдено')).toBeInTheDocument();
    });
  });

  it('calls patientApi.searchByModule with the icu roster after debounce', async () => {
    mockSearch.mockResolvedValue({ data: [] });
    renderSearch();
    const input = screen.getByLabelText('ПІБ, телефон або ID');
    await userEvent.type(input, 'Петр');
    await waitFor(() => {
      expect(mockSearch).toHaveBeenCalledWith('icu', 'Петр', expect.any(Object));
    });
  });

  it('shows an error with retry that refetches the roster', async () => {
    mockSearch
      .mockRejectedValueOnce({ response: { data: { message: 'MIS недоступна' } } })
      .mockResolvedValueOnce({ data: testPatients });
    renderSearch();
    const input = screen.getByLabelText('ПІБ, телефон або ID');
    await userEvent.type(input, 'Петр');

    expect(await screen.findByText('MIS недоступна')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Повторити' }));

    expect(await screen.findByText('Петренко Іван')).toBeInTheDocument();
    expect(mockSearch).toHaveBeenCalledTimes(2);
  });

  it('displays patient options in dropdown', async () => {
    mockSearch.mockResolvedValue({ data: testPatients });
    renderSearch();
    const input = screen.getByLabelText('ПІБ, телефон або ID');
    await userEvent.type(input, 'Петр');
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
      expect(screen.getByText('Коваленко Олена')).toBeInTheDocument();
    });
  });

  it('calls onSelect when a patient is chosen', async () => {
    mockSearch.mockResolvedValue({ data: testPatients });
    renderSearch();
    const input = screen.getByLabelText('ПІБ, телефон або ID');
    await userEvent.type(input, 'Петр');
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
    });
    await userEvent.click(screen.getByText('Петренко Іван'));
    await waitFor(() => {
      expect(onSelect).toHaveBeenCalledWith(testPatients[0]);
    });
  });

  it('shows loading indicator during search', async () => {
    mockSearch.mockReturnValue(new Promise(() => {}));
    renderSearch();
    const input = screen.getByLabelText('ПІБ, телефон або ID');
    await userEvent.type(input, 'Петр');
    await waitFor(() => {
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });
  });

  it('renders with custom label', () => {
    render(
      <ThemeModeProvider>
        <PatientSearch onSelect={onSelect} label="Знайти пацієнта" />
      </ThemeModeProvider>
    );
    expect(screen.getByLabelText('Знайти пацієнта')).toBeInTheDocument();
  });
});
