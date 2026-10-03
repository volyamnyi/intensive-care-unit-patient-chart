import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import AuditEventConsole from '../../components/common/AuditEventConsole';

const mockSearch = vi.fn();
const mockDetail = vi.fn();
const mockHistory = vi.fn();

vi.mock('../../api/platform', () => ({
  auditEventsApi: {
    search: (...args: unknown[]) => mockSearch(...args),
    detail: (...args: unknown[]) => mockDetail(...args),
    objectHistory: (...args: unknown[]) => mockHistory(...args),
  },
}));

vi.mock('../../utils/errorMessage', () => ({
  getErrorMessage: (_err: unknown, fallback: string) => fallback,
}));

const summaryRow = {
  auditId: '11111111-1111-1111-1111-111111111111',
  occurredAt: '2026-10-02T10:00:00Z',
  eventClass: 'USER_ACTIVITY',
  module: 'platform',
  functionalArea: 'users',
  action: 'platform.user.view',
  actionType: 'VIEW',
  criticality: 'LOW',
  actorType: 'USER',
  actorLogin: 'admin',
  targetType: 'User',
  targetId: '16',
  outcome: 'SUCCESS',
  correlationId: null,
  userActionId: null,
  parentAuditId: null,
};

const detail = {
  ...summaryRow,
  recordedAt: '2026-10-02T10:00:01Z',
  actorId: '16',
  actorDisplayName: 'Адмін',
  actorRoles: ['ADMINISTRATOR'],
  businessKey: null,
  errorCode: null,
  reasonCode: null,
  requestId: null,
  source: 'ADMIN_TOOL',
  httpMethod: 'GET',
  routeTemplate: '/api/admin/users/{id}',
  ipAddress: null,
  durationMs: 12,
  affectedRecords: 1,
  changes: [],
  targets: [{ relationType: 'PRIMARY', entityType: 'User', entityId: '16', businessKey: null }],
  children: [],
  externalCalls: [],
  metadata: {},
  integrityHash: 'abc123',
  integrityVerified: true,
  restrictedDetail: false,
};

function renderConsole() {
  return render(
    <ThemeModeProvider>
      <AuditEventConsole />
    </ThemeModeProvider>,
  );
}

describe('AuditEventConsole', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockSearch.mockResolvedValue({
      data: { content: [summaryRow], totalElements: 1, totalPages: 1, number: 0, size: 20 },
    });
    mockDetail.mockResolvedValue({ data: detail });
    mockHistory.mockResolvedValue({
      data: { entityType: 'User', entityId: '16', eventCount: 1, events: [summaryRow] },
    });
  });

  it('searches with combined filters and renders rows', async () => {
    renderConsole();
    await userEvent.click(screen.getByRole('button', { name: 'Пошук' }));
    await waitFor(() => {
      expect(mockSearch).toHaveBeenCalledWith(
        expect.objectContaining({ page: 0, size: 20 }),
      );
    });
    expect(await screen.findByText('platform.user.view')).toBeInTheDocument();
    expect(screen.getByText('Знайдено подій: 1')).toBeInTheDocument();
  });

  it('opens the detail card with integrity and relations on row click', async () => {
    renderConsole();
    await userEvent.click(screen.getByRole('button', { name: 'Пошук' }));
    await userEvent.click(await screen.findByText('platform.user.view'));
    await waitFor(() => {
      expect(mockDetail).toHaveBeenCalledWith('11111111-1111-1111-1111-111111111111');
    });
    expect(await screen.findByTestId('audit-event-detail')).toBeInTheDocument();
    expect(screen.getByText('цілісність підтверджено')).toBeInTheDocument();
    expect(screen.getByText(/Повʼязані обʼєкти/)).toBeInTheDocument();
  });

  it('loads object history with root markers', async () => {
    renderConsole();
    await userEvent.type(screen.getByPlaceholderText('Тип (напр. Episode)'), 'User');
    await userEvent.type(screen.getByPlaceholderText('ID обʼєкта'), '16');
    await userEvent.click(screen.getByRole('button', { name: 'Показати' }));
    await waitFor(() => {
      expect(mockHistory).toHaveBeenCalledWith('User', '16');
    });
    expect(await screen.findByTestId('audit-object-history')).toBeInTheDocument();
    expect(screen.getByText('коренева')).toBeInTheDocument();
  });

  it('shows an honest error when the backend denies console access', async () => {
    mockSearch.mockRejectedValue({ response: { status: 403 } });
    renderConsole();
    await userEvent.click(screen.getByRole('button', { name: 'Пошук' }));
    await waitFor(() => {
      expect(screen.getByText('Не вдалося завантажити події аудиту')).toBeInTheDocument();
    });
  });
});
