import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { render, act } from '@testing-library/react';
import { ProstheticsProvider, useProsthetics } from '../../prosthetics/ProstheticsContext';

const STORAGE_KEY = 'prosthetics:draft';

function TestComponent() {
  const ctx = useProsthetics();
  return (
    <div>
      <span data-testid="patient-id">{ctx.draft.patientId ?? 'null'}</span>
      <span data-testid="order-id">{ctx.draft.orderId ?? 'null'}</span>
      <span data-testid="template-id">{ctx.draft.templateId ?? 'null'}</span>
      <button
        data-testid="set-patient"
        onClick={() => ctx.setDraftField('patientId', 'pat-1')}
      >
        Set Patient
      </button>
      <button
        data-testid="set-order"
        onClick={() => ctx.setDraftField('orderId', 'order-1')}
      >
        Set Order
      </button>
      <button
        data-testid="reset"
        onClick={() => ctx.resetDraft()}
      >
        Reset
      </button>
    </div>
  );
}

describe('ProstheticsContext', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('initializes with empty draft', () => {
    const { getByTestId } = render(
      <ProstheticsProvider>
        <TestComponent />
      </ProstheticsProvider>
    );

    expect(getByTestId('patient-id').textContent).toBe('null');
    expect(getByTestId('order-id').textContent).toBe('null');
    expect(getByTestId('template-id').textContent).toBe('null');
  });

  it('setDraftField updates draft state', () => {
    const { getByTestId } = render(
      <ProstheticsProvider>
        <TestComponent />
      </ProstheticsProvider>
    );

    act(() => {
      getByTestId('set-patient').click();
    });

    expect(getByTestId('patient-id').textContent).toBe('pat-1');
  });

  it('setDraftField persists to sessionStorage', () => {
    const { getByTestId } = render(
      <ProstheticsProvider>
        <TestComponent />
      </ProstheticsProvider>
    );

    act(() => {
      getByTestId('set-patient').click();
    });

    const saved = JSON.parse(sessionStorage.getItem(STORAGE_KEY)!);
    expect(saved.patientId).toBe('pat-1');
  });

  it('resetDraft clears both state and sessionStorage', () => {
    const { getByTestId } = render(
      <ProstheticsProvider>
        <TestComponent />
      </ProstheticsProvider>
    );

    act(() => {
      getByTestId('set-patient').click();
      getByTestId('set-order').click();
    });

    expect(getByTestId('patient-id').textContent).toBe('pat-1');
    expect(getByTestId('order-id').textContent).toBe('order-1');

    act(() => {
      getByTestId('reset').click();
    });

    expect(getByTestId('patient-id').textContent).toBe('null');
    expect(getByTestId('order-id').textContent).toBe('null');
    expect(sessionStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('hydrates draft from sessionStorage on mount', () => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
      patientId: 'existing-patient',
      orderId: 'existing-order',
      templateId: null,
    }));

    const { getByTestId } = render(
      <ProstheticsProvider>
        <TestComponent />
      </ProstheticsProvider>
    );

    expect(getByTestId('patient-id').textContent).toBe('existing-patient');
    expect(getByTestId('order-id').textContent).toBe('existing-order');
    expect(getByTestId('template-id').textContent).toBe('null');
  });

  it('throws when used outside provider', () => {
    const originalError = console.error;
    console.error = () => {};

    expect(() => render(<TestComponent />)).toThrow(
      'useProsthetics must be used within ProstheticsProvider'
    );

    console.error = originalError;
  });
});
