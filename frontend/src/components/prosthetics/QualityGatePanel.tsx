import { useState } from 'react';
import { AlertTriangle, CheckCircle2, RotateCcw, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import type { FlowInstance, SnapshotStage } from '@/prosthetics/types';

interface QualityGatePanelProps {
  instance: FlowInstance;
  stage: SnapshotStage;
  isApprover: boolean;
  submitting: boolean;
  onPass: (criteriaConfirmed: string[]) => void;
  onRework: (comment: string) => void;
  onFail: (comment: string) => void;
}

export function QualityGatePanel({
  instance,
  stage,
  isApprover,
  submitting,
  onPass,
  onRework,
  onFail,
}: QualityGatePanelProps) {
  const gate = stage.gate;
  const criteria = gate?.checklist ?? [];
  const [checked, setChecked] = useState<Record<string, boolean>>({});
  const [comment, setComment] = useState('');

  const allChecked = criteria.every((c) => checked[c]);
  const requiresAdmin = gate?.requiredApproverRole === 'PROSTHETICS_ADMINISTRATOR';
  const locked = !isApprover && requiresAdmin;

  return (
    <div className="space-y-6">
      <div className="rounded-lg border-2 border-accent bg-accent/5 p-6">
        <div className="flex items-center gap-3">
          <AlertTriangle className="size-6 shrink-0 text-accent" />
          <div>
            <h1 className="font-display text-xl font-semibold">{gate?.name ?? 'Контрольна точка якості'}</h1>
            <p className="text-sm text-muted-foreground">
              Етап {stage.name} · спроб доопрацювання: {instance.reworkCount ?? 0}
            </p>
          </div>
        </div>
      </div>

      {locked && (
        <div className="flex items-start gap-3 rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm">
          <AlertTriangle className="mt-0.5 size-4 shrink-0 text-destructive" />
          <p>Рішення на цій контрольній точці приймає адміністратор протезування.</p>
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Критерії приймання</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {criteria.length === 0 && (
            <p className="text-sm text-muted-foreground">
              Критерії не задані. Підтвердіть проходження контрольного огляду.
            </p>
          )}
          {criteria.map((c, i) => {
            const fieldId = `gate-criterion-${i}`;
            return (
              <div key={c} className="flex items-start gap-3 rounded-md border p-3 transition-colors hover:bg-muted/30">
                <Checkbox
                  id={fieldId}
                  disabled={locked}
                  checked={checked[c] ?? false}
                  onCheckedChange={(v) => setChecked((s) => ({ ...s, [c]: v === true }))}
                />
                <Label htmlFor={fieldId} id={`${fieldId}-label`} className="text-sm leading-snug font-normal">
                  {c}
                </Label>
              </div>
            );
          })}
          <Textarea
            rows={3}
            disabled={locked}
            placeholder="Коментар контролера якості (обов'язково при відхиленні)"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
          />
        </CardContent>
      </Card>

      <div className="flex flex-wrap gap-3">
        <Button
          className="bg-success text-success-foreground hover:bg-success/90"
          disabled={locked || submitting || !allChecked}
          onClick={() => onPass(criteria.filter((c) => checked[c]))}
        >
          <CheckCircle2 className="size-4" /> Прийнято (Pass)
        </Button>
        <Button
          variant="outline"
          disabled={locked || submitting || !comment.trim()}
          onClick={() => onRework(comment.trim())}
        >
          <RotateCcw className="size-4" /> На доопрацювання
        </Button>
        <Button
          variant="destructive"
          disabled={locked || submitting || !comment.trim()}
          onClick={() => onFail(comment.trim())}
        >
          <XCircle className="size-4" /> Брак (Fail)
        </Button>
      </div>
      {!allChecked && (
        <p className="text-xs text-muted-foreground">
          Приймання можливе лише після підтвердження всіх критеріїв.
        </p>
      )}
    </div>
  );
}
