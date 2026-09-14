import { describe, it, expect } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import PpeNoticeBanner from '@/components/prosthetics/PpeNoticeBanner';
import {
  getPpeNotices,
  PPE_FULL_KIT_TEXT,
  PPE_FULL_KIT_TITLE,
  PPE_NITRILE_TEXT,
} from '@/prosthetics/ppeNotices';

const STEP_TRAINING_SOCKET = 'e0000024-0000-0000-0000-000000000024';

describe('PpeNoticeBanner', () => {
  it('full-kit: role=alert, title, точний текст, 4 фото з alt+підписами, warning-класи', () => {
    const [notice] = getPpeNotices(STEP_TRAINING_SOCKET);
    const { container } = render(<PpeNoticeBanner notice={notice!} />);
    const alert = screen.getByRole('alert');
    expect(alert).toHaveAttribute('data-testid', 'ppe-notice-full-kit');
    expect(alert.className).toContain('border-warning/50');
    expect(within(alert).getByText(PPE_FULL_KIT_TITLE)).toBeDefined();
    expect(within(alert).getByText(PPE_FULL_KIT_TEXT)).toBeDefined();
    const imgs = within(alert).getAllByRole('img');
    expect(imgs).toHaveLength(4);
    for (const img of imgs) {
      expect(img.getAttribute('src')).toMatch(/^\/ppe\/.+\.png$/);
      expect(img.getAttribute('alt')?.trim()).not.toBe('');
    }
    // підписи — короткі назви ЗІЗ
    for (const caption of ['Термостійкі рукавиці', 'Захисні окуляри', 'Респіратор', 'Навушники']) {
      expect(within(alert).getByText(caption)).toBeDefined();
    }
    expect(container.querySelectorAll('figure')).toHaveLength(4);
  });

  it('не містить інтерактивних елементів (інваріант wizard-checkbox-surface)', () => {
    const [notice] = getPpeNotices(STEP_TRAINING_SOCKET);
    const { container } = render(<PpeNoticeBanner notice={notice!} />);
    expect(container.querySelectorAll('button, a, input, select, textarea').length).toBe(0);
    expect(container.querySelectorAll('[role="checkbox"], [role="button"]').length).toBe(0);
  });

  it('nitrile: role=alert, title «Захист рук», точний текст, 1 фото, default-варіант', () => {
    const nitrile = getPpeNotices(STEP_TRAINING_SOCKET)[1];
    expect(nitrile?.kind).toBe('nitrile');
    const { container } = render(<PpeNoticeBanner notice={nitrile!} />);
    const alert = screen.getByRole('alert');
    expect(alert).toHaveAttribute('data-testid', 'ppe-notice-nitrile');
    // візуально відмінний від full-kit: базовий варіант без warning-бордера
    expect(alert.className).toContain('bg-card');
    expect(alert.className).not.toContain('border-warning/50');
    expect(within(alert).getByText('Захист рук')).toBeDefined();
    expect(within(alert).getByText(PPE_NITRILE_TEXT)).toBeDefined();
    const imgs = within(alert).getAllByRole('img');
    expect(imgs).toHaveLength(1);
    expect(imgs[0]?.getAttribute('src')).toBe('/ppe/nitrile-gloves.png');
    expect(imgs[0]?.getAttribute('alt')?.trim()).not.toBe('');
    expect(within(alert).getByText('Нітрилові рукавиці')).toBeDefined();
    expect(container.querySelectorAll('figure')).toHaveLength(1);
    expect(container.querySelectorAll('button, a, input, select, textarea').length).toBe(0);
    expect(container.querySelectorAll('[role="checkbox"], [role="button"]').length).toBe(0);
  });

  it('стек e0000024: обидва банери у порядку [full-kit, nitrile]', () => {
    const { container } = render(
      <>
        {getPpeNotices(STEP_TRAINING_SOCKET).map((n) => (
          <PpeNoticeBanner key={n.kind} notice={n} />
        ))}
      </>,
    );
    const alerts = container.querySelectorAll('[role="alert"]');
    expect(alerts).toHaveLength(2);
    expect(alerts[0]?.getAttribute('data-testid')).toBe('ppe-notice-full-kit');
    expect(alerts[1]?.getAttribute('data-testid')).toBe('ppe-notice-nitrile');
  });
});
