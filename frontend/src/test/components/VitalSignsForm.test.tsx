import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import VitalSignsForm from '../../components/common/VitalSignsForm';
import type { HourlyRecordCreateRequest } from '../../types';

const theme = createTheme({});
const emptyValues: HourlyRecordCreateRequest = { recordTime: '' };

function renderForm(props: Partial<Parameters<typeof VitalSignsForm>[0]> = {}) {
  const defaultProps = {
    values: emptyValues,
    onChange: vi.fn(),
    onSave: vi.fn(),
  };
  return render(
    <ThemeProvider theme={theme}>
      <VitalSignsForm {...defaultProps} {...props} />
    </ThemeProvider>
  );
}

describe('VitalSignsForm', () => {
  it('renders all vital sign input fields', () => {
    renderForm();
    expect(screen.getByLabelText('АТ сист (мм.рт.ст)')).toBeInTheDocument();
    expect(screen.getByLabelText('АТ діас (мм.рт.ст)')).toBeInTheDocument();
    expect(screen.getByLabelText('ЧСС (в 1 хв)')).toBeInTheDocument();
    expect(screen.getByLabelText('SpO2 (%)')).toBeInTheDocument();
    expect(screen.getByLabelText('Темп. тіла (°С)')).toBeInTheDocument();
    expect(screen.getByLabelText('ЦВТ (мм.вод.ст)')).toBeInTheDocument();
    expect(screen.getByLabelText('ЧД (в 1 хв)')).toBeInTheDocument();
    expect(screen.getByLabelText('Свідомість')).toBeInTheDocument();
    expect(screen.getByLabelText('etCO2 (мм.рт.ст)')).toBeInTheDocument();
    expect(screen.getByLabelText('FiO2 (%)')).toBeInTheDocument();
    expect(screen.getByLabelText('Діурез (мл/год)')).toBeInTheDocument();
    expect(screen.getByLabelText('Дренаж (мл)')).toBeInTheDocument();
    expect(screen.getByLabelText('Біль (0-10)')).toBeInTheDocument();
    expect(screen.getByLabelText('Нотатки')).toBeInTheDocument();
  });

  it('shows save button', () => {
    renderForm();
    expect(screen.getByText('Зберегти показники')).toBeInTheDocument();
  });

  it('disables save button when saving', () => {
    renderForm({ saving: true });
    expect(screen.getByText('Збереження...')).toBeDisabled();
  });

  it('calls onSave when save button clicked', async () => {
    const onSave = vi.fn();
    renderForm({ onSave });
    await userEvent.click(screen.getByText('Зберегти показники'));
    expect(onSave).toHaveBeenCalled();
  });

  it('calls onChange when a numeric field is edited', async () => {
    const onChange = vi.fn();
    renderForm({ onChange });
    const input = screen.getByLabelText('ЧСС (в 1 хв)');
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
});
