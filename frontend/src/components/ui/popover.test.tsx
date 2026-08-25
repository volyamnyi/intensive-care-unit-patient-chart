import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  Popover,
  PopoverContent,
  PopoverDescription,
  PopoverTitle,
  PopoverTrigger,
} from './popover';

function renderPopover() {
  return render(
    <Popover>
      <PopoverTrigger>Open popover</PopoverTrigger>
      <PopoverContent>
        <PopoverTitle>Popover title</PopoverTitle>
        <PopoverDescription>Popover description</PopoverDescription>
      </PopoverContent>
    </Popover>,
  );
}

describe('Popover', () => {
  it('does not render content while closed', () => {
    renderPopover();
    expect(screen.queryByText('Popover title')).not.toBeInTheDocument();
  });

  it('renders a trigger button with aria-expanded and opens on click', async () => {
    const user = userEvent.setup();
    renderPopover();
    const trigger = screen.getByRole('button', { name: 'Open popover' });
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    await user.click(trigger);
    expect(await screen.findByText('Popover title')).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Open popover' })).toHaveAttribute('aria-expanded', 'true'),
    );
  });

  it('renders the open popup as a labelled dialog', async () => {
    const user = userEvent.setup();
    renderPopover();
    await user.click(screen.getByRole('button', { name: 'Open popover' }));
    expect(await screen.findByRole('dialog', { name: 'Popover title' })).toBeInTheDocument();
    expect(screen.getByText('Popover description')).toBeInTheDocument();
  });

  it('closes on Escape', async () => {
    const user = userEvent.setup();
    renderPopover();
    await user.click(screen.getByRole('button', { name: 'Open popover' }));
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByText('Popover title')).not.toBeInTheDocument());
  });

  it('is controlled via the open prop', async () => {
    const { rerender } = render(
      <Popover open>
        <PopoverContent>
          <PopoverTitle>Always open</PopoverTitle>
        </PopoverContent>
      </Popover>,
    );
    expect(await screen.findByText('Always open')).toBeInTheDocument();
    rerender(
      <Popover open={false}>
        <PopoverContent>
          <PopoverTitle>Always open</PopoverTitle>
        </PopoverContent>
      </Popover>,
    );
    await waitFor(() => expect(screen.queryByText('Always open')).not.toBeInTheDocument());
  });
});
