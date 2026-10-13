import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';

import { PageContainer } from '../../layouts/PageContainer';

describe('PageContainer', () => {
  it('renders plain by page width (default)', () => {
    render(
      <PageContainer>
        <div data-testid="inside">test</div>
      </PageContainer>,
    );
    const el = screen.getByTestId('inside').parentElement;
    expect(el).toHaveClass('max-w-page', 'mx-auto', 'w-full', 'px-4', 'sm:px-6');
  });

  it('honours width=narrow', () => {
    render(
      <PageContainer width="narrow"><div data-testid="inside" /></PageContainer>,
    );
    expect(screen.getByTestId('inside').parentElement).toHaveClass('max-w-page-narrow');
  });

  it('honours width=wide', () => {
    render(
      <PageContainer width="wide"><div data-testid="inside" /></PageContainer>,
    );
    expect(screen.getByTestId('inside').parentElement).toHaveClass('max-w-page-wide');
  });

  it('honours width=full', () => {
    render(
      <PageContainer width="full"><div data-testid="inside" /></PageContainer>,
    );
    expect(screen.getByTestId('inside').parentElement).toHaveClass('max-w-none', 'w-full');
  });

  it('appends className overrides after the built-in classes', () => {
    render(
      <PageContainer className="my-4"><div data-testid="inside" /></PageContainer>,
    );
    expect(screen.getByTestId('inside').parentElement).toHaveClass('my-4');
  });
});
