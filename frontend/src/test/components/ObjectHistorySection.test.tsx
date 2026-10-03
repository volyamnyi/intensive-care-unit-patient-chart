import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import ObjectHistorySection from '../../components/common/ObjectHistorySection';

const mockHistory = vi.fn();

vi.mock('../../api/platform', () => ({
  auditEventsApi: {
    objectHistory: (...args: unknown[]) => mockHistory(...args),
  },
}));

vi.mock('../../utils/errorMessage', () => ({
  getErrorMessage: (_err: unknown, fallback: string) => fallback,
}));

const historyPayload = {
  entityType: 'Episode',
  entityId: 'a111',
  eventCount: 2,
  events: [
    {
      auditId: '11111111-1111-1111-1111-111111111111',
      occurredAt: '2026-10-02T10:00:00Z',
      eventClass: 'BUSINESS',
      module: 'icu',
      functionalArea: 'episode',
      action: 'icu.episode.create',
      actionType: 'CREATE',
      criticality: 'MEDIUM',
      actorType: 'USER',
      actorLogin: 'doctor1',
      targetType: 'Episode',
      targetId: 'a111',
      outcome: 'SUCCESS',
      correlationId: null,
      userActionId: null,
      parentAuditId: null,
    },
    {
      auditId: '22222222-2222-2222-2222-222222222222',
      occurredAt: '2026-10-02T11:00:00Z',
      eventClass: 'BUSINESS',
      module: 'icu',
      functionalArea: 'episode',
      action: 'icu.episode.update',
      actionType: 'UPDATE',
      criticality: 'MEDIUM',
      actorType: 'USER',
      actorLogin: 'doctor1',
      targetType: 'Episode',
      targetId: 'a111',
      outcome: 'SUCCESS',
      correlationId: null,
      userActionId: null,
      parentAuditId: '11111111-1111-1111-1111-111111111111',
    },
  ],
};

function renderSection() {
  return render(
    <ThemeModeProvider>
      <ObjectHistorySection entityType="Episode" entityId="a111" title="Історія змін епізоду" />
    </ThemeModeProvider>,
  );
}

describe('ObjectHistorySection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHistory.mockResolvedValue({ data: historyPayload });
  });

  it('renders chronological history with root/child markers', async () => {
    renderSection();
    expect(await screen.findByTestId('object-history-section')).toBeInTheDocument();
    expect(screen.getByText('Історія змін епізоду')).toBeInTheDocument();
    expect(screen.getByText('коренева')).toBeInTheDocument();
    expect(screen.getByText('дочірня')).toBeInTheDocument();
    expect(mockHistory).toHaveBeenCalledWith('Episode', 'a111');
  });

  it('shows an access hint on 403 instead of fabricating history', async () => {
    mockHistory.mockRejectedValue({ response: { status: 403 } });
    renderSection();
    expect(await screen.findByTestId('object-history-denied')).toBeInTheDocument();
    expect(screen.getByText('Історія змін доступна ролям із доступом до консолі аудиту.')).toBeInTheDocument();
  });
});
