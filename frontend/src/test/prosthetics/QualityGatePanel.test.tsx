import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QualityGatePanel } from '@/components/prosthetics/QualityGatePanel';
import type { FlowInstance, SnapshotStage } from '@/prosthetics/types';

const instance: FlowInstance = {
  id: 'inst-1',
  templateId: 'tpl-1',
  patientId: 'pat-1',
  orderId: 'ord-1',
  assignedUserId: null,
  status: 'WAITING_REVIEW',
  currentStageId: 'stage-1',
  currentStepId: null,
  currentExecutionId: null,
  templateName: 'TP-UL-01',
  patientPib: 'Сніжко Оксана Володимирівна',
  orderNumber: 'ПВ-26-0413',
  currentStageName: 'Контроль якості',
  currentStepName: null,
  startTime: null,
  endTime: null,
  totalActiveSeconds: null,
  totalIdleSeconds: null,
  reworkCount: 0,
  failReason: null,
  pausedAt: null,
  resumedAt: null,
  pauseCategory: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const stage: SnapshotStage = {
  id: 'stage-1',
  name: 'Здавання готового виробу',
  stageType: 'TECHNICAL',
  canSkip: false,
  requiresApproval: true,
  gate: {
    id: 'gate-1',
    name: 'Контрольна точка якості',
    requiredApproverRole: 'PROSTHETICS_ADMINISTRATOR',
    checklist: ['Виріб зібрано відповідно до креслення', 'Функціональні тести пройдено'],
    attachmentsRequired: false,
    reworkLoops: [],
  },
  steps: [],
};

const onPass = vi.fn();
const onRework = vi.fn();
const onFail = vi.fn();

function renderPanel(isApprover = true) {
  return render(
    <QualityGatePanel
      instance={instance}
      stage={stage}
      isApprover={isApprover}
      submitting={false}
      onPass={onPass}
      onRework={onRework}
      onFail={onFail}
    />
  );
}

describe('QualityGatePanel', () => {
  it('renders gate name, stage and criteria checklist', () => {
    renderPanel();
    expect(screen.getByText('Контрольна точка якості')).toBeInTheDocument();
    expect(screen.getByText(/Етап Здавання готового виробу/)).toBeInTheDocument();
    expect(screen.getByText('Виріб зібрано відповідно до креслення')).toBeInTheDocument();
    expect(screen.getByText('Функціональні тести пройдено')).toBeInTheDocument();
  });

  it('disables Pass until all criteria are checked', () => {
    renderPanel();
    const pass = screen.getByRole('button', { name: /Прийнято \(Pass\)/ });
    expect(pass).toBeDisabled();

    fireEvent.click(screen.getByRole('checkbox', { name: 'Виріб зібрано відповідно до креслення' }));
    expect(pass).toBeDisabled();

    fireEvent.click(screen.getByRole('checkbox', { name: 'Функціональні тести пройдено' }));
    expect(pass).toBeEnabled();
  });

  it('calls onPass with confirmed criteria when clicked', () => {
    renderPanel();
    fireEvent.click(screen.getByRole('checkbox', { name: 'Виріб зібрано відповідно до креслення' }));
    fireEvent.click(screen.getByRole('checkbox', { name: 'Функціональні тести пройдено' }));
    fireEvent.click(screen.getByRole('button', { name: /Прийнято \(Pass\)/ }));

    expect(onPass).toHaveBeenCalledWith([
      'Виріб зібрано відповідно до креслення',
      'Функціональні тести пройдено',
    ]);
  });

  it('requires a comment for rework and fail', () => {
    renderPanel();
    const rework = screen.getByRole('button', { name: /На доопрацювання/ });
    const fail = screen.getByRole('button', { name: /Брак \(Fail\)/ });
    expect(rework).toBeDisabled();
    expect(fail).toBeDisabled();

    fireEvent.change(screen.getByPlaceholderText(/Коментар контролера/), {
      target: { value: 'Потрібна корекція згину' },
    });
    expect(rework).toBeEnabled();
    expect(fail).toBeEnabled();

    fireEvent.click(rework);
    expect(onRework).toHaveBeenCalledWith('Потрібна корекція згину');

    fireEvent.click(fail);
    expect(onFail).toHaveBeenCalledWith('Потрібна корекція згину');
  });

  it('locks controls for non-approver when admin role required', () => {
    renderPanel(false);
    expect(
      screen.getByText('Рішення на цій контрольній точці приймає адміністратор протезування.')
    ).toBeInTheDocument();

    const pass = screen.getByRole('button', { name: /Прийнято \(Pass\)/ });
    const rework = screen.getByRole('button', { name: /На доопрацювання/ });
    const fail = screen.getByRole('button', { name: /Брак \(Fail\)/ });
    expect(pass).toBeDisabled();
    expect(rework).toBeDisabled();
    expect(fail).toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'Виріб зібрано відповідно до креслення' })).toHaveAttribute(
      'aria-disabled',
      'true'
    );
  });
});
