import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PatientPoolSection from './PatientPoolSection';
import { patientApi } from '../../api/platform';
import type { AxiosResponse } from 'axios';
import type { PageResponse, PatientDto } from '../../types/core';

vi.mock('../../api/platform', () => ({
  patientApi: {
    searchByModule: vi.fn(),
    getById: vi.fn(),
    getPatientPool: vi.fn(),
  },
}));

vi.mock('../../api/medication', () => ({
  prescriptionApi: {
    getByPatient: vi.fn(() => Promise.resolve({ data: [] })),
  },
}));

const mockedPool = vi.mocked(patientApi.getPatientPool);

function patient(id: number, over: Record<string, unknown> = {}): PatientDto {
  return {
    id,
    fullName: `Пацієнт ${id}`,
    birthDate: '',
    sexCode: '',
    address: '',
    phone: '',
    email: '',
    bloodGroup: '',
    rhFactor: '',
    departmentId: 19,
    room: '1',
    bed: '1',
    doctorName: '',
    ...over,
  };
}

function pageOf(rows: PatientDto[], totalPages = 1, number = 0): AxiosResponse<PageResponse<PatientDto>> {
  return {
    data: {
      content: rows,
      totalElements: rows.length,
      totalPages,
      number,
      size: 20,
    },
    status: 200,
    statusText: 'OK',
    headers: {},
    config: {} as AxiosResponse['config'],
  };
}

function renderSection(onOpenDrawer = vi.fn()) {
  return {
    ...render(<PatientPoolSection onOpenDrawer={onOpenDrawer} storageKey="test-pool" />),
    onOpenDrawer,
  };
}

describe('PatientPoolSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  async function openSection() {
    const utils = renderSection();
    await userEvent.click(screen.getByRole('button', { name: /Всі пацієнти/ }));
    return utils;
  }

  it('loads page 0 on open and pages forward/back', async () => {
    mockedPool.mockImplementation((params?: { query?: string; status?: string; page?: number; size?: number }) => {
      const page = params?.page ?? 0;
      const rows = page === 0 ? [patient(1), patient(2)] : [patient(3)];
      return Promise.resolve(pageOf(rows, 2, page));
    });
    const { onOpenDrawer } = await openSection();

    expect(await screen.findByText('Пацієнт 1')).toBeInTheDocument();
    expect(screen.getByText(/Сторінка 1 з 2/)).toBeInTheDocument();
    expect(mockedPool).toHaveBeenCalledWith(
      expect.objectContaining({ page: 0, size: 20 }),
      expect.any(AbortSignal),
    );

    await userEvent.click(screen.getByRole('button', { name: /Далі/ }));
    expect(await screen.findByText('Пацієнт 3')).toBeInTheDocument();
    expect(screen.getByText(/Сторінка 2 з 2/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Назад/ }));
    expect(await screen.findByText('Пацієнт 1')).toBeInTheDocument();
    expect(onOpenDrawer).not.toHaveBeenCalled();
  });

  it('shows MIS status badges with priority and opens the drawer', async () => {
    mockedPool.mockResolvedValue(
      pageOf([patient(7, { patientStatus: 'CMP' }), patient(8, { patientStatus: 'XYZ' })]),
    );
    const { onOpenDrawer } = await openSection();

    const discharged = screen.getByText('Пацієнт 7').closest('tr') as HTMLElement;
    expect(within(discharged).getByText('Виписано')).toBeInTheDocument();
    const unknown = screen.getByText('Пацієнт 8').closest('tr') as HTMLElement;
    expect(within(unknown).getByText('XYZ')).toBeInTheDocument();

    await userEvent.click(within(discharged).getByRole('button', { name: /Відкрити/ }));
    expect(onOpenDrawer).toHaveBeenCalledTimes(1);
    expect(onOpenDrawer.mock.calls[0][0]).toMatchObject({ id: 7 });
    expect(onOpenDrawer.mock.calls[0][1]).toEqual([]);
  });

  it('filters by status chip and resets to the first page', async () => {
    mockedPool.mockResolvedValue(pageOf([patient(9)]));
    await openSection();
    expect(await screen.findByText('Пацієнт 9')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Виписано' }));
    expect(mockedPool).toHaveBeenLastCalledWith(
      expect.objectContaining({ status: 'CMP', page: 0 }),
      expect.any(AbortSignal),
    );
  });

  it('searches by query and surfaces errors with retry', async () => {
    mockedPool.mockRejectedValueOnce(new Error('down'));
    mockedPool.mockResolvedValue(pageOf([]));
    await openSection();
    expect(await screen.findByText('Не вдалося завантажити пул пацієнтів')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Спробувати ще/ }));
    expect(mockedPool).toHaveBeenCalledTimes(2);

    await userEvent.type(screen.getByPlaceholderText(/Пошук за ПІБ/), '9999');
    await waitFor(() =>
      expect(mockedPool).toHaveBeenLastCalledWith(
        expect.objectContaining({ query: '9999' }),
        expect.any(AbortSignal),
      ),
    );
    expect(await screen.findByText('Пацієнтів не знайдено')).toBeInTheDocument();
  });
});
