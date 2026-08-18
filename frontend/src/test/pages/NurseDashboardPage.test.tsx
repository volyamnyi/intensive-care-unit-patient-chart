import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import NurseDashboardPage from '../../pages/nurse/NurseDashboardPage';

const mockSearch = vi.fn();

vi.mock('../../api/icu', () => ({
  episodeApi: { search: (...args: unknown[]) => mockSearch(...args) },
}));

const mockEpisode = { id: 'ep-1', patientName: 'Петренко Іван Сергійович', status: 'ACTIVE' as const };

function renderPage() {
  return render(
    <ThemeModeProvider>
      <MemoryRouter>
        <NurseDashboardPage />
      </MemoryRouter>
    </ThemeModeProvider>
  );
}

describe('NurseDashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading spinner initially', () => {
    mockSearch.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('renders empty state when no patients', async () => {
    mockSearch.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Немає активних пацієнтів')).toBeInTheDocument();
    });
  });

  it('renders patient list after loading', async () => {
    mockSearch.mockResolvedValue({ data: [mockEpisode] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Петренко Іван Сергійович')).toBeInTheDocument();
    });
  });

  it('sets document title', async () => {
    mockSearch.mockResolvedValue({ data: [mockEpisode] });
    renderPage();
    await waitFor(() => {
      expect(document.title).toBe('ВАІТ — Медсестра');
    });
  });
});
