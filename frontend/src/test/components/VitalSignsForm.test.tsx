import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import VitalSignsForm from '../../components/common/VitalSignsForm';
import type { HourlyRecordCreateRequest } from '../../types';

const emptyValues: HourlyRecordCreateRequest = { recordTime: '' };

function renderForm(props: Partial<Parameters<typeof VitalSignsForm>[0]> = {}) {
  const defaultProps = {
    values: emptyValues,
    onChange: vi.fn(),
    onSave: vi.fn(),
  };
  return render(
    <ThemeModeProvider>
      <VitalSignsForm {...defaultProps} {...props} />
    </ThemeModeProvider>
  );
}

describe('VitalSignsForm', () => {
  it('renders all vital sign input fields', () => {
    renderForm();
    expect(screen.getByLabelText('АТ сист. (мм рт.ст.)')).toBeInTheDocument();
    expect(screen.getByLabelText('АТ діас. (мм рт.ст.)')).toBeInTheDocument();
    expect(screen.getByLabelText('ЧСС (уд/хв)')).toBeInTheDocument();
    expect(screen.getByLabelText('SpO₂ (%)')).toBeInTheDocument();
    expect(screen.getByLabelText('Температура (°C)')).toBeInTheDocument();
    expect(screen.getByLabelText('ЦВТ (мм рт.ст.)')).toBeInTheDocument();
    expect(screen.getByLabelText('ЧД (дих/хв)')).toBeInTheDocument();
    expect(screen.getByLabelText('Свідомість')).toBeInTheDocument();
    expect(screen.getByLabelText('EtCO₂ (мм рт.ст.)')).toBeInTheDocument();
    expect(screen.getByLabelText('FiO₂ (%)')).toBeInTheDocument();
    expect(screen.getByLabelText('Діурез (мл)')).toBeInTheDocument();
    expect(screen.getByLabelText('Дренаж (мл)')).toBeInTheDocument();
    expect(screen.getByLabelText('Біль (0-10)')).toBeInTheDocument();
    expect(screen.getByLabelText('Примітки')).toBeInTheDocument();
  });

  it('shows save button', () => {
    renderForm();
    expect(screen.getByText('Зберегти')).toBeInTheDocument();
  });

  it('disables save button when saving', () => {
    renderForm({ saving: true });
    expect(screen.getByText('Збереження...')).toBeDisabled();
  });

  it('calls onSave when save button clicked', async () => {
    const onSave = vi.fn();
    renderForm({ onSave });
    await userEvent.click(screen.getByText('Зберегти'));
    expect(onSave).toHaveBeenCalled();
  });

  it('calls onChange when a numeric field is edited', async () => {
    const onChange = vi.fn();
    renderForm({ onChange });
    const input = screen.getByLabelText('ЧСС (уд/хв)');
    await userEvent.type(input, '80');
    expect(onChange).toHaveBeenCalled();
  });

  it('calls onChange when a text field is edited', async () => {
    const onChange = vi.fn();
    renderForm({ onChange });
    const input = screen.getByLabelText('Свідомість');
    await userEvent.type(input, 'Ясна');
    expect(onChange).toHaveBeenCalled();
  });

  it('displays current values', () => {
    const values: HourlyRecordCreateRequest = {
      recordTime: '',
      heartRate: 75,
      systolicBP: 120,
      temperature: 36.6,
    };
    renderForm({ values });
    expect(screen.getByDisplayValue('75')).toBeInTheDocument();
    expect(screen.getByDisplayValue('120')).toBeInTheDocument();
    expect(screen.getByDisplayValue('36.6')).toBeInTheDocument();
  });

  it('renders optional title', () => {
    renderForm({ title: 'Показники 8:00' });
    expect(screen.getByText('Показники 8:00')).toBeInTheDocument();
  });

  it('renders without title when not provided', () => {
    const { container } = renderForm();
    const headings = container.querySelectorAll('h6');
    expect(headings.length).toBe(0);
  });

  describe('clinical range validation', () => {
    const ranges = {
      temperature: { min: 34, max: 42 },
      heartRate: { min: 0, max: 300 },
      respiratoryRate: { min: 0, max: 60 },
      systolicBP: { min: 50, max: 250 },
      diastolicBP: { min: 30, max: 150 },
      spo2: { min: 50, max: 100 },
    };

    function isValid(field: keyof typeof ranges, value: number | null | undefined): boolean {
      if (value == null) return true;
      const r = ranges[field];
      return value >= r.min && value <= r.max;
    }

    it('normal values pass validation', () => {
      expect(isValid('temperature', 36.6)).toBe(true);
      expect(isValid('heartRate', 80)).toBe(true);
      expect(isValid('respiratoryRate', 16)).toBe(true);
      expect(isValid('systolicBP', 120)).toBe(true);
      expect(isValid('diastolicBP', 80)).toBe(true);
      expect(isValid('spo2', 98)).toBe(true);
    });

    it('out-of-range high values fail', () => {
      expect(isValid('temperature', 43)).toBe(false);
      expect(isValid('heartRate', 350)).toBe(false);
      expect(isValid('respiratoryRate', 70)).toBe(false);
      expect(isValid('systolicBP', 260)).toBe(false);
      expect(isValid('diastolicBP', 160)).toBe(false);
      expect(isValid('spo2', 110)).toBe(false);
    });

    it('out-of-range low values fail', () => {
      expect(isValid('temperature', 33)).toBe(false);
      expect(isValid('heartRate', -1)).toBe(false);
      expect(isValid('respiratoryRate', -1)).toBe(false);
      expect(isValid('systolicBP', 40)).toBe(false);
      expect(isValid('diastolicBP', 20)).toBe(false);
      expect(isValid('spo2', 40)).toBe(false);
    });

    it('boundary values pass validation', () => {
      expect(isValid('temperature', 34)).toBe(true);
      expect(isValid('temperature', 42)).toBe(true);
      expect(isValid('heartRate', 0)).toBe(true);
      expect(isValid('heartRate', 300)).toBe(true);
      expect(isValid('systolicBP', 50)).toBe(true);
      expect(isValid('systolicBP', 250)).toBe(true);
      expect(isValid('diastolicBP', 30)).toBe(true);
      expect(isValid('diastolicBP', 150)).toBe(true);
    });

    it('null and undefined values pass validation', () => {
      expect(isValid('temperature', null)).toBe(true);
      expect(isValid('temperature', undefined)).toBe(true);
      expect(isValid('heartRate', null)).toBe(true);
      expect(isValid('respiratoryRate', undefined)).toBe(true);
    });
  });
});
