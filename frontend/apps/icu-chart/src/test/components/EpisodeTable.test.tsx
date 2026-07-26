import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EpisodeTable from '../../components/common/EpisodeTable';
import type { Episode } from '../../types';

const mockEpisodes: Episode[] = [
  {
    id: 'ep-1',
    patientId: 1001,
    patientName: 'Петренко Іван',
    hospitalizationId: null,
    departmentId: null,
    admissionDate: '2025-06-01T10:00:00Z',
    dischargeDate: null,
    status: 'ACTIVE',
    heightCm: null,
    ward: null,
    bedNumber: null,
    admissionDiagnosis: null,
    attendingDoctorId: null,
    createdBy: 1,
    createdAt: '2025-06-01T10:00:00Z',
    updatedBy: 1,
    updatedAt: '2025-06-01T10:00:00Z',
    version: 1,
  },
  {
    id: 'ep-2',
    patientId: 1002,
    patientName: 'Коваленко Олена',
    hospitalizationId: null,
    departmentId: null,
    admissionDate: '2025-06-02T14:00:00Z',
    dischargeDate: '2025-06-10T10:00:00Z',
    status: 'COMPLETED',
    heightCm: null,
    ward: null,
    bedNumber: null,
    admissionDiagnosis: null,
    attendingDoctorId: null,
    createdBy: 1,
    createdAt: '2025-06-02T14:00:00Z',
    updatedBy: 1,
    updatedAt: '2025-06-10T10:00:00Z',
    version: 1,
  },
];

function renderTable(props: Partial<React.ComponentProps<typeof EpisodeTable>> = {}) {
  return render(
    <EpisodeTable
      episodes={props.episodes ?? []}
      onSelect={props.onSelect}
      loading={props.loading}
    />
  );
}

describe('EpisodeTable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows loading state', () => {
    renderTable({ loading: true });
    expect(screen.getByText('Завантаження...')).toBeInTheDocument();
  });

  it('shows "Немає даних" empty state when not loading', () => {
    renderTable({ episodes: [], loading: false });
    expect(screen.getByText('Немає даних')).toBeInTheDocument();
  });

  it('renders episode rows with patient name', () => {
    renderTable({ episodes: mockEpisodes });
    expect(screen.getByText('Петренко Іван')).toBeInTheDocument();
    expect(screen.getByText('Коваленко Олена')).toBeInTheDocument();
  });

  it('shows status chips with correct labels', () => {
    renderTable({ episodes: mockEpisodes });
    expect(screen.getByText('Активний')).toBeInTheDocument();
    expect(screen.getByText('Завершений')).toBeInTheDocument();
  });

  it('shows admission and discharge dates', () => {
    renderTable({ episodes: mockEpisodes });
    expect(screen.getByText('01.06.2025')).toBeInTheDocument();
    expect(screen.getByText('10.06.2025')).toBeInTheDocument();
  });

  it('shows dash for episodes without discharge date', () => {
    renderTable({ episodes: [mockEpisodes[0]] });
    const cells = screen.getAllByText('-');
    expect(cells.length).toBeGreaterThan(0);
  });

  it('calls onSelect when row is clicked', async () => {
    const onSelect = vi.fn();
    renderTable({ episodes: mockEpisodes, onSelect });
    await userEvent.click(screen.getByText('Петренко Іван'));
    expect(onSelect).toHaveBeenCalledWith(mockEpisodes[0]);
  });

  it('shows "Відкрити" link when onSelect is provided', () => {
    renderTable({ episodes: mockEpisodes, onSelect: vi.fn() });
    const links = screen.getAllByText('Відкрити');
    expect(links).toHaveLength(2);
  });

  it('does not show "Відкрити" link when onSelect is not provided', () => {
    renderTable({ episodes: mockEpisodes });
    expect(screen.queryByText('Відкрити')).not.toBeInTheDocument();
  });
});
