import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProductionPage, { formatDurationSeconds } from '@/pages/prosthetics/ProductionPage';
import type { ProductionSummary, ProductionWorkItem } from '@/prosthetics/types';

const productionApiMock = vi.hoisted(() => ({
  list: vi.fn(),
  summary: vi.fn(),
  detail: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  productionApi: productionApiMock,
}));

let mockCanViewAll = true;

vi.mock('@/services/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 5, login: 'prosthetist1', fullName: 'Іваненко Іван', role: 'PROSTHETIST' },
    isAuthenticated: true,
    loading: false,
    hasRole: (role: string) => role === 'PROSTHETIST',
    hasPermission: (permission: string) =>
      permission === 'PROSTHETICS_PRODUCTION_VIEW'
      || (mockCanViewAll && permission === 'PROSTHETICS_PRODUCTION_VIEW_ALL'),
  }),
}));

const item = (overrides: Partial<ProductionWorkItem> = {}): ProductionWorkItem => ({
  instanceId: 'i1',
  orderId: 'o1',
  patientId: '900001',
  patientPib: 'Бондаренко Тарас',
  prosthetistUserId: 5,
  prosthetistFullName: 'Іваненко Іван',
  orderNumber: 'MIS-900001-55',
  productCode: '06 24 09',
  productType: 'LOWER_LIMB',
  prosthesisType: 'Модульний',
  prescriptionDate: '2026-09-01',
  templateName: 'TP-UL-01',
  currentStageName: 'Формування',
  currentStepName: 'Моделювання',
  status: 'IN_PROGRESS',
  startTime: '2026-09-05T08:00:00',
  endTime: null,
  lastActivityAt: '2026-09-09T08:00:00',
  createdAt: '2026-09-05T08:00:00',
  updatedAt: '2026-09-09T08:00:00',
  elapsedSeconds: 345600,
  activeSeconds: 13740,
  idleSeconds: 2100,
  expectedActiveSeconds: null,
  activeDeviationSeconds: null,
  brakCount: 1,
  reworkCount: 0,
  failed: false,
  attentionFlags: [],
  ...overrides,
});

const summary: ProductionSummary = {
  totalItems: 2,
  inWork: 2,
  active: 2,
  paused: 0,
  completed: 0,
  failed: 0,
  brakItems: 1,
  reworkItems: 0,
  avgElapsedSeconds: 172800,
  avgActiveSeconds: 9000,
};

function mockOk(items: ProductionWorkItem[] = [item()], total = items.length) {
  productionApiMock.list.mockResolvedValue({
    data: { content: items, totalElements: total, totalPages: 1, number: 0, size: 20 },
  });
  productionApiMock.summary.mockResolvedValue({ data: summary });
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/prosthetics/production']}>
      <Routes>
        <Route path="/prosthetics/production" element={<ProductionPage />} />
        <Route path="/prosthetics/process/:id/wizard" element={<div>Wizard Page</div>} />
        <Route path="/prosthetics/process/:id/done" element={<div>Done Page</div>} />
        <Route path="/prosthetics/process/:id/failed" element={<div>Failed Page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProductionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCanViewAll = true;
  });

  it('renders KPI cards and rows after load', async () => {
    mockOk();
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Бондаренко Тарас')).toBeInTheDocument();
    });
    expect(screen.getByText('Моніторинг виробництва')).toBeInTheDocument();
    expect(screen.getByText('В роботі')).toBeInTheDocument();
    expect(screen.getByText('06 24 09')).toBeInTheDocument();
  });

  it('renders the prosthetist column before the product column', async () => {
    mockOk();
    const { container } = renderPage();
    await waitFor(() => {
      expect(screen.getByText('Бондаренко Тарас')).toBeInTheDocument();
    });
    const headers = within(container.querySelector('table') as HTMLElement)
      .getAllByRole('columnheader')
      .map((h) => h.textContent);
    expect(headers.indexOf('Протезист')).toBeGreaterThanOrEqual(0);
    expect(headers.indexOf('Виріб')).toBeGreaterThan(headers.indexOf('Протезист'));
    expect(headers.indexOf('Пацієнт')).toBeGreaterThan(headers.indexOf('Виріб'));
  });

  it('shows skeletons while loading', () => {
    productionApiMock.list.mockReturnValue(new Promise(() => {}));
    productionApiMock.summary.mockReturnValue(new Promise(() => {}));
    const { container } = renderPage();
    expect(container.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0);
  });

  it('shows an error with retry and reloads on retry', async () => {
    productionApiMock.list.mockRejectedValueOnce(new Error('boom'));
    productionApiMock.summary.mockResolvedValue({ data: summary });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Помилка')).toBeInTheDocument();
    });
    mockOk();
    fireEvent.click(screen.getByRole('button', { name: 'Спробувати знову' }));
    await waitFor(() => {
      expect(screen.getByText('Бондаренко Тарас')).toBeInTheDocument();
    });
  });

  it('shows an empty state when there are no rows', async () => {
    productionApiMock.list.mockResolvedValue({
      data: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 },
    });
    productionApiMock.summary.mockResolvedValue({ data: { ...summary, totalItems: 0 } });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Немає виробів за поточними фільтрами')).toBeInTheDocument();
    });
  });

  it('passes the quality filter to the API', async () => {
    const user = userEvent.setup();
    mockOk();
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Бондаренко Тарас')).toBeInTheDocument();
    });
    await user.click(screen.getByRole('combobox', { name: 'Якість' }));
    await user.click(await screen.findByRole('option', { name: 'Є брак' }));
    await waitFor(() => {
      const last = productionApiMock.list.mock.calls.at(-1)?.[0] as { quality: string };
      expect(last.quality).toBe('BRAK');
    });
  });

  it('hides the scope selector without VIEW_ALL and sends no assigneeId', async () => {
    mockCanViewAll = false;
    mockOk();
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Бондаренко Тарас')).toBeInTheDocument();
    });
    expect(screen.queryByRole('combobox', { name: 'Чиї вироби' })).not.toBeInTheDocument();
    const last = productionApiMock.list.mock.calls.at(-1)?.[0] as { assigneeId?: number };
    expect(last.assigneeId).toBeUndefined();
  });

  it('paginates forward', async () => {
    mockOk([item()], 45);
    productionApiMock.list.mockResolvedValueOnce({
      data: {
        content: [item()],
        totalElements: 45,
        totalPages: 3,
        number: 0,
        size: 20,
      },
    });
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/Сторінка 1 з 3/)).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Далі' }));
    await waitFor(() => {
      const last = productionApiMock.list.mock.calls.at(-1)?.[0] as { page: number };
      expect(last.page).toBe(1);
    });
  });

  it('opens the detail drawer on open', async () => {
    mockOk();
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Бондаренко Тарас')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Відкрити' }));
    await waitFor(() => {
      expect(productionApiMock.detail).toHaveBeenCalledWith('i1', expect.anything());
    });
  });
});

describe('formatDurationSeconds', () => {
  it('formats the spec examples', () => {
    expect(formatDurationSeconds(0)).toBe('0:00');
    expect(formatDurationSeconds(222)).toBe('0:03');
    expect(formatDurationSeconds(3 * 3600 + 42 * 60)).toBe('3:42');
    expect(formatDurationSeconds(5 * 3600 + 17 * 60)).toBe('5:17');
    expect(formatDurationSeconds(13740)).toBe('3:49');
    expect(formatDurationSeconds(90000)).toBe('1д 1:00');
    expect(formatDurationSeconds(null)).toBe('0:00');
  });
});
