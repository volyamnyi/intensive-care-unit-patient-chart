import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Dialog, DialogContent, DialogTitle } from './dialog';

function renderDialog(props: { open?: boolean; mobileFullscreen?: boolean } = {}) {
  return render(
    <Dialog open={props.open ?? true}>
      <DialogContent mobileFullscreen={props.mobileFullscreen}>
        <DialogTitle>Test dialog</DialogTitle>
        <p>Dialog body</p>
      </DialogContent>
    </Dialog>,
  );
}

describe('Dialog', () => {
  it('does not render content while closed', () => {
    renderDialog({ open: false });
    expect(screen.queryByText('Test dialog')).not.toBeInTheDocument();
  });

  it('renders an open labelled dialog', async () => {
    renderDialog();
    expect(await screen.findByRole('dialog', { name: 'Test dialog' })).toBeInTheDocument();
    expect(screen.getByText('Dialog body')).toBeInTheDocument();
  });

  it('carries the consolidated tablet max-width policy classes', async () => {
    renderDialog();
    const popup = await screen.findByRole('dialog', { name: 'Test dialog' });
    expect(popup).toHaveClass('sm:max-w-sm');
    expect(popup).toHaveClass('md:max-w-md');
  });

  it('does not set data-fullscreen by default', async () => {
    renderDialog();
    const popup = await screen.findByRole('dialog', { name: 'Test dialog' });
    expect(popup).not.toHaveAttribute('data-fullscreen');
  });

  it('marks mobile-fullscreen dialogs via data-fullscreen="mobile"', async () => {
    renderDialog({ mobileFullscreen: true });
    const popup = await screen.findByRole('dialog', { name: 'Test dialog' });
    expect(popup).toHaveAttribute('data-fullscreen', 'mobile');
  });

  it('closes on Escape when controlled-open allows it', async () => {
    const user = userEvent.setup();
    const { rerender } = renderDialog();
    expect(await screen.findByRole('dialog', { name: 'Test dialog' })).toBeInTheDocument();
    let open = true;
    rerender(
      <Dialog
        open={open}
        onOpenChange={(next) => {
          open = next;
        }}
      >
        <DialogContent>
          <DialogTitle>Test dialog</DialogTitle>
          <p>Dialog body</p>
        </DialogContent>
      </Dialog>,
    );
    await user.keyboard('{Escape}');
    await waitFor(() => expect(open).toBe(false));
  });

  it('is controlled via the open prop', async () => {
    const { rerender } = renderDialog({ open: true });
    expect(await screen.findByText('Dialog body')).toBeInTheDocument();
    rerender(
      <Dialog open={false}>
        <DialogContent>
          <DialogTitle>Test dialog</DialogTitle>
          <p>Dialog body</p>
        </DialogContent>
      </Dialog>,
    );
    await waitFor(() => expect(screen.queryByText('Dialog body')).not.toBeInTheDocument());
  });
});
