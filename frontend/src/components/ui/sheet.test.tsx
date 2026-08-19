import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from './sheet';

function renderSheet(side?: 'left' | 'right' | 'top' | 'bottom') {
  return render(
    <Sheet>
      <SheetTrigger>Open sheet</SheetTrigger>
      <SheetContent side={side}>
        <SheetHeader>
          <SheetTitle>Sheet title</SheetTitle>
          <SheetDescription>Sheet description</SheetDescription>
        </SheetHeader>
      </SheetContent>
    </Sheet>,
  );
}

describe('Sheet', () => {
  it('does not render content while closed', () => {
    renderSheet();
    expect(screen.queryByText('Sheet title')).not.toBeInTheDocument();
  });

  it('opens on trigger click and shows content', async () => {
    const user = userEvent.setup();
    renderSheet();
    await user.click(screen.getByRole('button', { name: 'Open sheet' }));
    expect(await screen.findByText('Sheet title')).toBeInTheDocument();
    expect(screen.getByText('Sheet description')).toBeInTheDocument();
  });

  it('renders a modal dialog with a labelled title', async () => {
    const user = userEvent.setup();
    renderSheet();
    await user.click(screen.getByRole('button', { name: 'Open sheet' }));
    expect(await screen.findByRole('dialog', { name: 'Sheet title' })).toBeInTheDocument();
  });

  it('closes via the close button', async () => {
    const user = userEvent.setup();
    renderSheet();
    await user.click(screen.getByRole('button', { name: 'Open sheet' }));
    await user.click(await screen.findByRole('button', { name: 'Close' }));
    await waitFor(() => expect(screen.queryByText('Sheet title')).not.toBeInTheDocument());
  });

  it('closes on Escape', async () => {
    const user = userEvent.setup();
    renderSheet();
    await user.click(screen.getByRole('button', { name: 'Open sheet' }));
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByText('Sheet title')).not.toBeInTheDocument());
  });

  it('defaults to the right side', async () => {
    const user = userEvent.setup();
    renderSheet();
    await user.click(screen.getByRole('button', { name: 'Open sheet' }));
    const content = await screen.findByText('Sheet title');
    await waitFor(() => expect(content.closest('[data-slot="sheet-content"]')).toHaveAttribute('data-side', 'right'));
  });

  it('applies the left side variant', async () => {
    const user = userEvent.setup();
    renderSheet('left');
    await user.click(screen.getByRole('button', { name: 'Open sheet' }));
    const content = await screen.findByText('Sheet title');
    await waitFor(() => expect(content.closest('[data-slot="sheet-content"]')).toHaveAttribute('data-side', 'left'));
  });

  it('applies the top side variant', async () => {
    const user = userEvent.setup();
    renderSheet('top');
    await user.click(screen.getByRole('button', { name: 'Open sheet' }));
    const content = await screen.findByText('Sheet title');
    await waitFor(() => expect(content.closest('[data-slot="sheet-content"]')).toHaveAttribute('data-side', 'top'));
  });

  it('is controlled via the open prop', async () => {
    const { rerender } = render(
      <Sheet open>
        <SheetContent>
          <SheetTitle>Always open</SheetTitle>
        </SheetContent>
      </Sheet>,
    );
    expect(await screen.findByText('Always open')).toBeInTheDocument();
    rerender(
      <Sheet open={false}>
        <SheetContent>
          <SheetTitle>Always open</SheetTitle>
        </SheetContent>
      </Sheet>,
    );
    await waitFor(() => expect(screen.queryByText('Always open')).not.toBeInTheDocument());
  });
});