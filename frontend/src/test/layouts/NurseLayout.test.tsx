import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import NurseLayout from '../../layouts/NurseLayout';

describe('NurseLayout', () => {
  it('renders without crashing', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/nurse']}>
        <NurseLayout />
      </MemoryRouter>
    );
    expect(container.firstChild).toBeInTheDocument();
  });
});
