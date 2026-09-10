import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProductionNormativeSettings from '@/components/prosthetics/ProductionNormativeSettings';

const normativeApiMock = vi.hoisted(() => ({
  getNormative: vi.fn(),
  updateNormative: vi.fn(),
}));

vi.mock('@/api/prosthetics', () => ({
  productionApi: normativeApiMock,
}));

const mockToastSuccess = vi.hoisted(() => vi.fn());
const mockToastError = vi.hoisted(() => vi.fn());

vi.mock('sonner', () => ({
  toast: {
    success: (...args: unknown[]) => mockToastSuccess(...args),
    error: (...args: unknown[]) => mockToastError(...args),
  },
}));

function renderSettings() {
  return render(
    <MemoryRouter>
      <ProductionNormativeSettings />
    </MemoryRouter>,
  );
}

describe('ProductionNormativeSettings', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    normativeApiMock.getNormative.mockResolvedValue({
      data: { overdueMultiplier: 1.5, staleDays: 7 },
    });
    normativeApiMock.updateNormative.mockResolvedValue({
      data: { overdueMultiplier: 2, staleDays: 3 },
    });
  });

  it('loads and shows the stored values', async () => {
    renderSettings();
    await waitFor(() => {
      expect(screen.getByLabelText(/Прострочення/)).toHaveValue(1.5);
    });
    expect(screen.getByLabelText(/Застій без активності/)).toHaveValue(7);
  });

  it('saves valid values with a toast', async () => {
    renderSettings();
    await waitFor(() => {
      expect(screen.getByLabelText(/Прострочення/)).toBeInTheDocument();
    });
    fireEvent.change(screen.getByLabelText(/Прострочення/), { target: { value: '2' } });
    fireEvent.change(screen.getByLabelText(/Застій без активності/), { target: { value: '3' } });
    fireEvent.click(screen.getByRole('button', { name: 'Зберегти' }));
    await waitFor(() => {
      expect(normativeApiMock.updateNormative).toHaveBeenCalledWith({
        overdueMultiplier: 2,
        staleDays: 3,
      });
    });
    expect(mockToastSuccess).toHaveBeenCalledWith('Нормативи збережено');
  });

  it('rejects out-of-range values without calling the API', async () => {
    renderSettings();
    await waitFor(() => {
      expect(screen.getByLabelText(/Прострочення/)).toBeInTheDocument();
    });
    fireEvent.change(screen.getByLabelText(/Прострочення/), { target: { value: '0.5' } });
    fireEvent.click(screen.getByRole('button', { name: 'Зберегти' }));
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(normativeApiMock.updateNormative).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText(/Прострочення/), { target: { value: '1.5' } });
    fireEvent.change(screen.getByLabelText(/Застій без активності/), { target: { value: '99' } });
    fireEvent.click(screen.getByRole('button', { name: 'Зберегти' }));
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(normativeApiMock.updateNormative).not.toHaveBeenCalled();
  });

  it('shows an error with retry on load failure', async () => {
    normativeApiMock.getNormative.mockRejectedValueOnce(new Error('boom'));
    renderSettings();
    await waitFor(() => {
      expect(screen.getByText('Помилка')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Спробувати знову' }));
    await waitFor(() => {
      expect(screen.getByLabelText(/Прострочення/)).toBeInTheDocument();
    });
  });

  it('toasts API save errors', async () => {
    normativeApiMock.updateNormative.mockRejectedValueOnce(new Error('boom'));
    renderSettings();
    await waitFor(() => {
      expect(screen.getByLabelText(/Прострочення/)).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: 'Зберегти' }));
    await waitFor(() => {
      expect(mockToastError).toHaveBeenCalled();
    });
  });
});
