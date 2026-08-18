import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeModeProvider } from '../../../styles/ThemeContext';
import AllergyWarning from '../../../components/prescription/AllergyWarning';
import type { AllergyItem } from '../../../types/medication';

const allergies: AllergyItem[] = [
  { id: 'a1', patientId: 1001, allergenName: 'Penicillin', sourceDocumentId: 1 },
  { id: 'a2', patientId: 1001, allergenName: 'Iodine', sourceDocumentId: 2 },
];

function renderWarning(medicineName: string) {
  return render(
    <ThemeModeProvider>
      <AllergyWarning medicineName={medicineName} allergies={allergies} />
    </ThemeModeProvider>
  );
}

describe('AllergyWarning', () => {
  it('renders nothing when no medicine', () => {
    const { container } = renderWarning('');
    expect(container.firstChild).toBeNull();
  });

  it('renders warning for matching allergy', () => {
    renderWarning('Penicillin');
    expect(screen.getByText(/Penicillin/)).toBeInTheDocument();
  });

  it('renders warning for partial match', () => {
    renderWarning('Iodine-based contrast');
    expect(screen.getByText(/Iodine/)).toBeInTheDocument();
  });

  it('renders nothing when no match', () => {
    const { container } = renderWarning('Paracetamol');
    expect(container.firstChild).toBeNull();
  });
});
