import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import userEvent from '@testing-library/user-event';
import ProductionTrendChart from '@/components/prosthetics/ProductionTrendChart';

const trendApiMock = vi.hoisted(() => ({
  list: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  productionApi: trendApiMock,
}));

let mockQualityView = true;

vi.mock('@/services/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 9, login: 'admin', fullName: 'Адмін', role: 'PROSTHETICS_ADMINISTRATOR' },
    hasPermission: (permission: string) =>
      permission === 'PROSTHETICS_PRODUCTION_VIEW_ALL'
      || (mockQualityView && permission === 'PROSTHETICS_PRODUCTION_QUALITY_VIEW'),
  }),
}));

vi.mock('@/hooks/useMediaQuery', () => ({
  useMediaQuery: () => true,
  useIsMobile: () => false,
}));

const row = (overrides = {}) => ({
  instanceId: 'i1',
  orderId: 'o1',
  patientId: '900001',
  patientPib: 'ПІБ',
  prosthetistUserId: 5,
  prosthetistFullName: 'Протезист',
  orderNumber: 'MIS-900001-55',
  productCode: '06 24 09',
  productType: 'LOWER_LIMB',
  prosthesisType: null,
  prescriptionDate: null,
  templateName: null,
  currentStageName: null,
  currentStepName: null,
  status: 'IN_PROGRESS',
  startTime: new Date().toISOString(),
  endTime: null,
  lastActivityAt: null,
  createdAt: new Date().toISOString(),
  updatedAt: null,
  elapsedSeconds: 0,
  activeSeconds: 0,
  idleSeconds: 0,
  expectedActiveSeconds: null,
  activeDeviationSeconds: null,
  brakCount: 0,
  reworkCount: 0,
  failed: false,
  attentionFlags: [],
  ...overrides,
});

function renderChart() {
  return render(
    <MemoryRouter>
      <ProductionTrendChart />
    </MemoryRouter>,
  );
}

describe('ProductionTrendChart', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockQualityView = true;
    trendApiMock.list.mockResolvedValue({ data: { content: [row()], totalElements: 1 } });
  });

  it('renders three series with QUALITY_VIEW and two without', async () => {
    const { unmount } = renderChart();
    await waitFor(() => {
      expect(screen.getByText('Провалено')).toBeInTheDocument();
    });
    expect(screen.getByText('Створено')).toBeInTheDocument();
    expect(screen.getByText('Завершено')).toBeInTheDocument();
    unmount();

    mockQualityView = false;
    renderChart();
    await waitFor(() => {
      expect(screen.getByText('Створено')).toBeInTheDocument();
    });
    expect(screen.queryByText('Провалено')).not.toBeInTheDocument();
  });

  it('shows an empty state when all buckets are zero', async () => {
    trendApiMock.list.mockResolvedValue({ data: { content: [], totalElements: 0 } });
    renderChart();
    await waitFor(() => {
      expect(screen.getByText('Немає даних за період')).toBeInTheDocument();
    });
  });

  it('shows an error with retry', async () => {
    trendApiMock.list.mockRejectedValueOnce(new Error('boom'));
    renderChart();
    await waitFor(() => {
      expect(screen.getByText('Помилка')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Спробувати знову' }));
    await waitFor(() => {
      expect(screen.getByText('Створено')).toBeInTheDocument();
    });
  });

  it('switches ranges without refetching failures', async () => {
    const user = userEvent.setup();
    renderChart();
    await waitFor(() => {
      expect(screen.getByText('Створено')).toBeInTheDocument();
    });
    expect(trendApiMock.list).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('tab', { name: '7д' }));
    expect(screen.getByRole('tab', { name: '7д' })).toHaveAttribute('data-active');
    expect(trendApiMock.list).toHaveBeenCalledTimes(1);
  });
});
