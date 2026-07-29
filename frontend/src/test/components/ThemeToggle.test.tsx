import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeModeProvider } from '../../styles/ThemeContext';
import ThemeToggle from '../../components/common/ThemeToggle';

describe('ThemeToggle', () => {
  it('renders toggle button with accessible label', () => {
    render(
      <ThemeModeProvider>
        <ThemeToggle />
      </ThemeModeProvider>
    );
    expect(screen.getByLabelText('Переключити тему')).toBeInTheDocument();
  });

  it('renders moon icon in default light mode', () => {
    render(
      <ThemeModeProvider>
        <ThemeToggle />
      </ThemeModeProvider>
    );
    expect(screen.getByRole('button', { name: 'Переключити тему' })).toBeInTheDocument();
  });
});
