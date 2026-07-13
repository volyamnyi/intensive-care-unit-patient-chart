import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { theme } from '../../styles/theme';
import DashboardPage from '../../pages/doctor/DashboardPage';

const mockSearch = vi.fn();

vi.mock('../../api/endpoints', () => ({
  episodeApi: { search: (...args: unknown[]) => mockSearch(...args) },
}));

const mockEpisodes = [
  { id: 'ep-1', patientName: 'Петренко Іван', status: 'ACTIVE' },
  { id: 'ep-2', patientName: 'Коваленко Олена', status: 'ACTIVE' },
  { id: 'ep-3', patientName: 'Сидоренко Василь', status: 'ACTIVE' },
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
    mockSearch.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('renders patient cards from API', async () => {
    mockSearch.mockResolvedValue({ data: mockEpisodes });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
      expect(screen.getByText('Коваленко Олена')).toBeInTheDocument();
      expect(screen.getByText('Сидоренко Василь')).toBeInTheDocument();
    });
  });

  it('search filters by patient name', async () => {
    mockSearch.mockResolvedValue({ data: mockEpisodes });
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
    mockSearch.mockResolvedValue({ data: mockEpisodes });
    renderPage();
    await waitFor(() => expect(screen.getByText('Петренко Іван')).toBeInTheDocument());
    const search = screen.getByPlaceholderText('Пошук пацієнта за ПІБ...');
    await userEvent.type(search, 'NonExistentName');
    await waitFor(() => {
      expect(screen.getByText('Немає пацієнтів за запитом')).toBeInTheDocument();
    });
  });

  it('shows alert when there are no cards at all', async () => {
    mockSearch.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Немає активних пацієнтів')).toBeInTheDocument();
    });
  });

  it('clearing search restores all cards', async () => {
    mockSearch.mockResolvedValue({ data: mockEpisodes });
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
