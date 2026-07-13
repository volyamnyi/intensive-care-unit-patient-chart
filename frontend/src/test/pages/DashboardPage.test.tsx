import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { theme } from '../../styles/theme';
import DashboardPage from '../../pages/doctor/DashboardPage';

const mockGetActive = vi.fn();

vi.mock('../../api/endpoints', () => ({
  icuCardApi: { getActive: () => mockGetActive() },
}));

const mockCards = [
  { id: 1, patientName: 'Петренко Іван', diagnosis: 'Пневмонія', apacheIi: 12, sofa: 4, status: 'ACTIVE', icuDays: [{ id: 1, dayNumber: 1, status: 'ACTIVE', date: '2026-07-13' }] },
  { id: 2, patientName: 'Коваленко Олена', diagnosis: 'Сепсис', apacheIi: 18, sofa: 6, status: 'ACTIVE', icuDays: [{ id: 2, dayNumber: 2, status: 'ACTIVE', date: '2026-07-13' }] },
  { id: 3, patientName: 'Сидоренко Василь', diagnosis: 'Інсульт', apacheIi: 22, sofa: 8, status: 'ACTIVE', icuDays: [{ id: 3, dayNumber: 1, status: 'ACTIVE', date: '2026-07-13' }] },
];

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    </ThemeProvider>
  );
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading spinner initially', () => {
    mockGetActive.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('renders patient cards from API', async () => {
    mockGetActive.mockResolvedValue({ data: mockCards });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
      expect(screen.getByText('Коваленко Олена')).toBeInTheDocument();
      expect(screen.getByText('Сидоренко Василь')).toBeInTheDocument();
    });
  });

  it('search filters by patient name', async () => {
    mockGetActive.mockResolvedValue({ data: mockCards });
    renderPage();
    await waitFor(() => expect(screen.getByText('Петренко Іван')).toBeInTheDocument());
    const search = screen.getByPlaceholderText('Пошук пацієнта за ПІБ...');
    await userEvent.type(search, 'Петренко');
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
      expect(screen.queryByText('Коваленко Олена')).not.toBeInTheDocument();
    });
  });

  it('shows different message for no results when search is active', async () => {
    mockGetActive.mockResolvedValue({ data: mockCards });
    renderPage();
    await waitFor(() => expect(screen.getByText('Петренко Іван')).toBeInTheDocument());
    const search = screen.getByPlaceholderText('Пошук пацієнта за ПІБ...');
    await userEvent.type(search, 'NonExistentName');
    await waitFor(() => {
      expect(screen.getByText('Немає пацієнтів за запитом')).toBeInTheDocument();
    });
  });

  it('shows alert when there are no cards at all', async () => {
    mockGetActive.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Немає активних пацієнтів')).toBeInTheDocument();
    });
  });

  it('clearing search restores all cards', async () => {
    mockGetActive.mockResolvedValue({ data: mockCards });
    renderPage();
    await waitFor(() => expect(screen.getByText('Петренко Іван')).toBeInTheDocument());
    const search = screen.getByPlaceholderText('Пошук пацієнта за ПІБ...');
    await userEvent.type(search, 'NonExistent');
    await waitFor(() => expect(screen.getByText('Немає пацієнтів за запитом')).toBeInTheDocument());
    await userEvent.clear(search);
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
      expect(screen.getByText('Коваленко Олена')).toBeInTheDocument();
    });
  });
});
