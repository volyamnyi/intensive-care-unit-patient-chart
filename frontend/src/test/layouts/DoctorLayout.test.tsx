import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import DoctorLayout from '../../layouts/DoctorLayout';

describe('DoctorLayout', () => {
  it('renders without crashing', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/doctor']}>
        <DoctorLayout />
      </MemoryRouter>
    );
    expect(container.firstChild).toBeInTheDocument();
  });
});
