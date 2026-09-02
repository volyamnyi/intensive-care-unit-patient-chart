import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import {
  LowerLimbMeasurementForm,
  LOWER_LIMB_ELEMENT_IDS,
  LOWER_LIMB_DIAGRAM_IDS,
  countFilledLowerLimbDiagram,
  countFilledLowerLimbAll,
} from '@/pages/prosthetics/process/LowerLimbMeasurementForm';

describe('LowerLimbMeasurementForm', () => {
  it('renders header fields and diagram section', () => {
    render(<LowerLimbMeasurementForm values={{}} onChange={vi.fn()} />);
    expect(screen.getByText('Бланк замірів №')).toBeInTheDocument();
    expect(screen.getByLabelText('Номер бланку замірів')).toBeInTheDocument();
    expect(screen.getByLabelText('Дата')).toBeInTheDocument();
    // ПІБ appears as label and via Field component
    expect(screen.getByLabelText('П.І.Б')).toBeInTheDocument();
    expect(screen.getByLabelText('Адреса')).toBeInTheDocument();
    expect(screen.getByLabelText('Шифр виробу')).toBeInTheDocument();
    expect(screen.getByLabelText('Найменування виробу')).toBeInTheDocument();
    expect(screen.getByLabelText('Рівень мобільності')).toBeInTheDocument();
    expect(screen.getByLabelText('Стать')).toBeInTheDocument();
    expect(screen.getByLabelText('Вік')).toBeInTheDocument();
    expect(screen.getByLabelText('Зріст')).toBeInTheDocument();
    expect(screen.getByLabelText('Вага')).toBeInTheDocument();
    expect(screen.getByLabelText('Примітки')).toBeInTheDocument();

    expect(screen.getByText('Обʼємний розмір та довжина кукси')).toBeInTheDocument();
    expect(screen.getByAltText('Схема замірів кукси та нижніх кінцівок')).toBeInTheDocument();
    // diagram boxes – sample checks
    expect(screen.getByLabelText('Стегно, R')).toBeInTheDocument();
    expect(screen.getByLabelText('Стегно, L')).toBeInTheDocument();
    expect(screen.getByLabelText('Обхват гомілки')).toBeInTheDocument();
    expect(screen.getByLabelText('Таз R, рівень 15')).toBeInTheDocument();
    expect(screen.getAllByLabelText(/Стегно|Коліно|Таз|Обхват|Довжина|Висота/)).toHaveLength(30);

    expect(screen.getByLabelText('Висота каблука')).toBeInTheDocument();
    expect(screen.getByLabelText('Розмір стопи')).toBeInTheDocument();
    expect(screen.getByLabelText('Комплектуючі')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Друк' })).toBeInTheDocument();
  });

  it('binds values and calls onChange with UUID keys', () => {
    const onChange = vi.fn();
    const values: Record<string, unknown> = {
      [LOWER_LIMB_ELEMENT_IDS.pib]: 'Іваненко І. І.',
      [LOWER_LIMB_ELEMENT_IDS.a_r]: '24',
    };
    render(<LowerLimbMeasurementForm values={values} onChange={onChange} />);
    expect(screen.getByLabelText('П.І.Б')).toHaveValue('Іваненко І. І.');
    expect(screen.getByLabelText('Стегно, R')).toHaveValue('24');

    fireEvent.change(screen.getByLabelText('Вік'), { target: { value: '45' } });
    expect(onChange).toHaveBeenCalledWith(LOWER_LIMB_ELEMENT_IDS.age, '45');

    fireEvent.change(screen.getByLabelText('Обхват гомілки'), { target: { value: '32' } });
    expect(onChange).toHaveBeenCalledWith(LOWER_LIMB_ELEMENT_IDS.b_calf, '32');
  });

  it('filters numeric diagram inputs to digits, dots and commas', () => {
    const onChange = vi.fn();
    render(<LowerLimbMeasurementForm values={{}} onChange={onChange} />);
    const input = screen.getByLabelText('Стегно, R') as HTMLInputElement;
    fireEvent.change(input, { target: { value: '12abc-3.,5' } });
    // should strip letters and dash, keep digits, dots, commas
    expect(onChange).toHaveBeenCalledWith(LOWER_LIMB_ELEMENT_IDS.a_r, '123.,5');
  });

  it('renders errors with destructive styling', () => {
    const errors: Record<string, string> = {
      [LOWER_LIMB_ELEMENT_IDS.pib]: 'Required',
      [LOWER_LIMB_ELEMENT_IDS.a_r]: 'Error',
    };
    render(<LowerLimbMeasurementForm values={{}} onChange={vi.fn()} errors={errors} />);
    expect(screen.getByLabelText('П.І.Б')).toHaveClass('border-destructive');
    expect(screen.getByLabelText('Стегно, R')).toHaveClass('!border-destructive');
  });

  it('respects disabled prop', () => {
    render(<LowerLimbMeasurementForm values={{}} onChange={vi.fn()} disabled />);
    expect(screen.getByLabelText('П.І.Б')).toBeDisabled();
    expect(screen.getByLabelText('Стегно, R')).toBeDisabled();
    expect(screen.getByLabelText('Висота каблука')).toBeDisabled();
  });

  it('has correct select options for gender', () => {
    render(<LowerLimbMeasurementForm values={{}} onChange={vi.fn()} />);
    const select = screen.getByLabelText('Стать') as HTMLSelectElement;
    expect(select.options).toHaveLength(3); // placeholder + 2
    expect(screen.getByRole('option', { name: 'Чоловіча' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Жіноча' })).toBeInTheDocument();
  });

  it('counts filled diagram values correctly', () => {
    expect(countFilledLowerLimbDiagram({})).toBe(0);
    expect(
      countFilledLowerLimbDiagram({
        [LOWER_LIMB_ELEMENT_IDS.a_r]: '12',
        [LOWER_LIMB_ELEMENT_IDS.a_l]: '13',
        [LOWER_LIMB_ELEMENT_IDS.b_calf]: '',
      }),
    ).toBe(2);
    expect(
      countFilledLowerLimbDiagram({
        [LOWER_LIMB_ELEMENT_IDS.a_r]: '12',
        [LOWER_LIMB_ELEMENT_IDS.a_l]: '13',
        [LOWER_LIMB_ELEMENT_IDS.b_calf]: '14',
      }),
    ).toBe(3);
    // header fields do not count toward diagram
    expect(
      countFilledLowerLimbDiagram({
        [LOWER_LIMB_ELEMENT_IDS.pib]: 'Test',
        [LOWER_LIMB_ELEMENT_IDS.age]: '45',
      }),
    ).toBe(0);
  });

  it('counts all filled values', () => {
    expect(
      countFilledLowerLimbAll({
        [LOWER_LIMB_ELEMENT_IDS.pib]: 'Test',
        [LOWER_LIMB_ELEMENT_IDS.a_r]: '12',
      }),
    ).toBe(2);
  });

  it('exposes correct diagram id count', () => {
    expect(LOWER_LIMB_DIAGRAM_IDS).toHaveLength(30);
  });

  it('print button calls window.print', () => {
    const printSpy = vi.spyOn(window, 'print').mockImplementation(() => {});
    render(<LowerLimbMeasurementForm values={{}} onChange={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: 'Друк' }));
    expect(printSpy).toHaveBeenCalled();
    printSpy.mockRestore();
  });
});
