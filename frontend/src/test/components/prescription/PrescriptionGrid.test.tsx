import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeModeProvider } from '../../../styles/ThemeContext';
import PrescriptionGrid from '../../../components/prescription/PrescriptionGrid';
import type { PrescriptionItem, PrescriptionDayPart, AllergyItem } from '../../../types/medication';

const mockItems: PrescriptionItem[] = [
  {
    id: 'item-1',
    listId: 'list-1',
    medicineName: 'Dopamine',
    medicineMethod: 'IV',
    regime: 'stat',
    status: 'Active',
    sortOrder: 0,
    dayParts: [
      {
        id: 'dp-1',
        dayId: 'day-1',
        itemId: 'item-1',
        period: 'morning',
        dose: '5mg',
        isPlanned: true,
        isPlannedFinished: false,
        isCompleted: false,
        isCompletedFinished: false,
        dayDate: '2026-07-29',
        doctorName: null,
        nurseName: null,
      } as PrescriptionDayPart,
    ],
  },
];

const mockAllergies: AllergyItem[] = [];

function renderGrid(props: Partial<React.ComponentProps<typeof PrescriptionGrid>> = {}) {
  return render(
    <ThemeModeProvider>
      <PrescriptionGrid
        items={props.items ?? mockItems}
        canEdit={props.canEdit ?? true}
        isDoctor={props.isDoctor ?? true}
        isNurse={props.isNurse ?? false}
        onPlan={props.onPlan ?? vi.fn()}
        onCancel={props.onCancel ?? vi.fn()}
        onExecute={props.onExecute}
        onAddItem={props.onAddItem ?? vi.fn()}
        onRemoveItem={props.onRemoveItem ?? vi.fn()}
        onSearchMedicine={props.onSearchMedicine ?? vi.fn().mockResolvedValue([])}
        allergies={props.allergies ?? mockAllergies}
        loading={props.loading}
      />
    </ThemeModeProvider>
  );
}

describe('PrescriptionGrid', () => {
  it('renders medicine name', () => {
    renderGrid();
    expect(screen.getByText('Dopamine')).toBeInTheDocument();
  });

  it('renders dose cell', () => {
    renderGrid();
    expect(screen.getByText('5mg')).toBeInTheDocument();
  });

  it('renders add medicine input', () => {
    renderGrid();
    expect(screen.getByPlaceholderText('Препарат')).toBeInTheDocument();
  });

  it('renders in loading state', () => {
    renderGrid({ loading: true });
    expect(document.querySelector('.animate-spin')).toBeInTheDocument();
  });
});
