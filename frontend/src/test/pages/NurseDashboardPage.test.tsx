import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from '@mui/material';
import { theme } from '../../styles/theme';
import NurseDashboardPage from '../../pages/nurse/NurseDashboardPage';

const mockGetActive = vi.fn();
const mockGetVitals = vi.fn();
const mockGetByCard = vi.fn();
const mockGetBalance = vi.fn();
const mockSaveVitals = vi.fn();
const mockAddOutput = vi.fn();

vi.mock('../../api/endpoints', () => ({
  icuCardApi: { getActive: () => mockGetActive() },
  icuDayApi: {
    getVitals: () => mockGetVitals(),
    getBalance: () => mockGetBalance(),
    saveVitals: () => mockSaveVitals(),
    addOutput: () => mockAddOutput(),
  },
  prescriptionApi: {
    getByCard: () => mockGetByCard(),
    execute: vi.fn(),
  },
}));

const mockCard = {
  id: 1,
  patientName: 'Петренко Іван Сергійович',
  diagnosis: 'Пневмонія',
  apacheIi: 12,
  sofa: 4,
  status: 'ACTIVE' as const,
  icuDays: [{ id: 1, dayNumber: 1, status: 'ACTIVE' as const, date: '2026-07-13' }],
};

function renderPage() {
  return render(
    <ThemeProvider theme={theme}>
      <NurseDashboardPage />
    </ThemeProvider>
  );
}

describe('NurseDashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading spinner initially', () => {
    mockGetActive.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('renders empty state when no patients', async () => {
    mockGetActive.mockResolvedValue({ data: [] });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Немає активних пацієнтів')).toBeInTheDocument();
    });
  });

  it('renders patient select and vitals form after loading', async () => {
    mockGetActive.mockResolvedValue({ data: [mockCard] });
    mockGetVitals.mockResolvedValue({ data: [] });
    mockGetByCard.mockResolvedValue({ data: [] });
    mockGetBalance.mockResolvedValue({ data: { totalIntake: 0, totalOutput: 0, dailyBalance: 0, cumulativeBalance: 0 } });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText((c) => c.startsWith('Показники —'))).toBeInTheDocument();
    });
    expect(screen.getByText((c) => c.startsWith('Втрати рідини —'))).toBeInTheDocument();
  });

  it('shows vitals form fields with type number and validation', async () => {
    mockGetActive.mockResolvedValue({ data: [mockCard] });
    mockGetVitals.mockResolvedValue({ data: [] });
    mockGetByCard.mockResolvedValue({ data: [] });
    mockGetBalance.mockResolvedValue({ data: { totalIntake: 0, totalOutput: 0, dailyBalance: 0, cumulativeBalance: 0 } });
    renderPage();
    const sysField = await screen.findByLabelText('АТ сист (мм.рт.ст)');
    expect(sysField).toHaveAttribute('type', 'number');
    expect(sysField).toHaveAttribute('min', '60');
    expect(sysField).toHaveAttribute('max', '300');
    const tempField = screen.getByLabelText('Темп. тіла (°С)');
    expect(tempField).toHaveAttribute('min', '30');
    expect(tempField).toHaveAttribute('max', '45');
  });

  it('sets document title', async () => {
    mockGetActive.mockResolvedValue({ data: [mockCard] });
    mockGetVitals.mockResolvedValue({ data: [] });
    mockGetByCard.mockResolvedValue({ data: [] });
    mockGetBalance.mockResolvedValue({ data: { totalIntake: 0, totalOutput: 0, dailyBalance: 0, cumulativeBalance: 0 } });
    renderPage();
    await waitFor(() => {
      expect(document.title).toBe('ВАІТ — Медсестра');
    });
  });

  it('shows snackbar after saving vitals', async () => {
    mockGetActive.mockResolvedValue({ data: [mockCard] });
    mockGetVitals.mockResolvedValue({ data: [] });
    mockGetByCard.mockResolvedValue({ data: [] });
    mockGetBalance.mockResolvedValue({ data: { totalIntake: 0, totalOutput: 0, dailyBalance: 0, cumulativeBalance: 0 } });
    mockSaveVitals.mockResolvedValue({ data: {} });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText((c) => c.startsWith('Показники —'))).toBeInTheDocument();
    });
    const saveButton = screen.getByRole('button', { name: 'Зберегти показники' });
    await userEvent.click(saveButton);
    await waitFor(() => {
      expect(screen.getByText('Показники збережено')).toBeInTheDocument();
    });
  });
});
