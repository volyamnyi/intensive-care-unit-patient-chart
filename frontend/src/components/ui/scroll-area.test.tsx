import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ScrollArea } from './scroll-area';

describe('ScrollArea', () => {
  it('renders the viewport with children', () => {
    render(
      <ScrollArea className="h-64">
        <p>Вміст області прокрутки</p>
      </ScrollArea>,
    );
    expect(screen.getByText('Вміст області прокрутки')).toBeInTheDocument();
    const viewport = screen.getByText('Вміст області прокрутки').closest('[data-slot="scroll-area-viewport"]');
    expect(viewport).not.toBeNull();
  });

  it('renders both scrollbars mounted with thumbs', () => {
    render(
      <ScrollArea>
        <p>Вміст</p>
      </ScrollArea>,
    );
    const scrollbars = document.querySelectorAll('[data-slot="scroll-area-scrollbar"]');
    expect(scrollbars).toHaveLength(2);
    expect(document.querySelectorAll('[data-slot="scroll-area-thumb"]')).toHaveLength(2);
  });
});