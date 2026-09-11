import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  LowerLimbMeasurementForm,
  LOWER_LIMB_ELEMENT_IDS,
  LOWER_LIMB_DIAGRAM_IDS,
  countFilledLowerLimbDiagram,
  countFilledLowerLimbAll,
} from '@/pages/prosthetics/process/LowerLimbMeasurementForm';
import { LOWER_LIMB_MOBILITY_OPTIONS } from '@/prosthetics/lowerLimbPrefill';

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
    expect(screen.getByRole('combobox', { name: 'Рівень мобільності' })).toBeInTheDocument();
    expect(screen.getByLabelText('Стать')).toBeInTheDocument();
    expect(screen.getByLabelText('Вік')).toBeInTheDocument();
    expect(screen.getByLabelText('Зріст')).toBeInTheDocument();
    expect(screen.getByLabelText('Вага')).toBeInTheDocument();
    expect(screen.getByLabelText('Примітки')).toBeInTheDocument();

    expect(screen.getByText(/Об.*ємний розмір та довжина кукси/)).toBeInTheDocument();
    expect(screen.getByAltText('Схема замірів кукси та нижніх кінцівок')).toBeInTheDocument();
    // diagram boxes – sample checks
    expect(screen.getByLabelText('Стегно, R')).toBeInTheDocument();
    expect(screen.getByLabelText('Стегно, L')).toBeInTheDocument();
    expect(screen.getByLabelText('Обхват гомілки')).toBeInTheDocument();
    expect(screen.getByLabelText('Таз R, рівень 15')).toBeInTheDocument();
    expect(document.querySelectorAll('input.diagram-input')).toHaveLength(30);

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
    // diagram inputs are now text with filtering, so value stays string

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

  it('renders mobility as a dropdown with exactly the 5 agreed options', async () => {
    const user = userEvent.setup();
    render(<LowerLimbMeasurementForm values={{}} onChange={vi.fn()} />);
    const trigger = screen.getByRole('combobox', { name: 'Рівень мобільності' });
    expect(trigger).toHaveTextContent('Рівень');
    await user.click(trigger);
    const listbox = await screen.findByRole('listbox');
    const options = within(listbox).getAllByRole('option');
    expect(options.map((o) => o.textContent)).toEqual([...LOWER_LIMB_MOBILITY_OPTIONS]);
    expect(options).toHaveLength(5);
  });

  it('selects a mobility option via onChange with the UUID key', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<LowerLimbMeasurementForm values={{}} onChange={onChange} />);
    await user.click(screen.getByRole('combobox', { name: 'Рівень мобільності' }));
    await user.click(await screen.findByRole('option', { name: '2 рівень' }));
    expect(onChange).toHaveBeenCalledWith(LOWER_LIMB_ELEMENT_IDS.mobilityLevel, '2 рівень');
  });

  it('displays the existing mobility value', () => {
    render(
      <LowerLimbMeasurementForm
        values={{ [LOWER_LIMB_ELEMENT_IDS.mobilityLevel]: '3 рівень' }}
        onChange={vi.fn()}
      />,
    );
    expect(screen.getByRole('combobox', { name: 'Рівень мобільності' })).toHaveTextContent(
      '3 рівень',
    );
  });

  it('disables the mobility dropdown when disabled', () => {
    render(<LowerLimbMeasurementForm values={{}} onChange={vi.fn()} disabled />);
    expect(screen.getByRole('combobox', { name: 'Рівень мобільності' })).toBeDisabled();
  });

  it('marks the mobility trigger with destructive styling on error', () => {
    render(
      <LowerLimbMeasurementForm
        values={{}}
        onChange={vi.fn()}
        errors={{ [LOWER_LIMB_ELEMENT_IDS.mobilityLevel]: 'Required' }}
      />,
    );
    expect(screen.getByRole('combobox', { name: 'Рівень мобільності' })).toHaveClass(
      'border-destructive',
    );
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
