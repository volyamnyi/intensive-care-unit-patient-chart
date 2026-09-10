import { useCallback, useEffect, useRef, useState } from 'react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { productionApi } from '@/api/prosthetics';
import { getErrorMessage } from '@/utils/errorMessage';

const MIN_MULTIPLIER = 1.0;
const MAX_MULTIPLIER = 5.0;
const MIN_STALE_DAYS = 1;
const MAX_STALE_DAYS = 30;

/**
 * Editable overdue/stale thresholds (epic #271, issue #277).
 * Rendered by the dashboard page only for VIEW_ALL holders; the backend
 * enforces the same gate and audits every change.
 */
export default function ProductionNormativeSettings() {
  const [multiplier, setMultiplier] = useState('1.5');
  const [staleDays, setStaleDays] = useState('7');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const fetchNormative = useCallback(async () => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    setLoading(true);
    setError(null);
    try {
      const res = await productionApi.getNormative(controller.signal);
      setMultiplier(String(res.data.overdueMultiplier));
      setStaleDays(String(res.data.staleDays));
    } catch (err) {
      if ((err as { code?: string })?.code === 'ERR_CANCELED') return;
      setError(getErrorMessage(err, 'Не вдалося завантажити нормативи'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchNormative();
    return () => abortRef.current?.abort();
  }, [fetchNormative]);

  const handleSave = async () => {
    const parsedMultiplier = Number(multiplier.replace(',', '.'));
    const parsedDays = Number(staleDays);
    if (
      !Number.isFinite(parsedMultiplier)
      || parsedMultiplier < MIN_MULTIPLIER
      || parsedMultiplier > MAX_MULTIPLIER
    ) {
      setFormError(`Множник має бути числом від ${MIN_MULTIPLIER} до ${MAX_MULTIPLIER}`);
      return;
    }
    if (!Number.isInteger(parsedDays) || parsedDays < MIN_STALE_DAYS || parsedDays > MAX_STALE_DAYS) {
      setFormError(`Поріг застою має бути цілим числом від ${MIN_STALE_DAYS} до ${MAX_STALE_DAYS}`);
      return;
    }
    setFormError(null);
    setSaving(true);
    try {
      const res = await productionApi.updateNormative({
        overdueMultiplier: parsedMultiplier,
        staleDays: parsedDays,
      });
      setMultiplier(String(res.data.overdueMultiplier));
      setStaleDays(String(res.data.staleDays));
      toast.success('Нормативи збережено');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося зберегти нормативи'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card className="mt-6">
      <CardHeader>
        <CardTitle className="text-base">Нормативи уваги</CardTitle>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="flex flex-wrap gap-2">
            <Skeleton className="h-10 w-48" />
            <Skeleton className="h-10 w-48" />
            <Skeleton className="h-10 w-32" />
          </div>
        ) : error ? (
          <Alert variant="destructive">
            <AlertTitle>Помилка</AlertTitle>
            <AlertDescription className="flex flex-wrap items-center gap-2">
              {error}
              <Button variant="outline" size="sm" onClick={() => void fetchNormative()}>
                Спробувати знову
              </Button>
            </AlertDescription>
          </Alert>
        ) : (
          <div>
            <div className="flex flex-wrap items-end gap-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="normative-multiplier">
                  Прострочення: elapsed &gt; expected × K
                </Label>
                <Input
                  id="normative-multiplier"
                  type="number"
                  inputMode="decimal"
                  min={MIN_MULTIPLIER}
                  max={MAX_MULTIPLIER}
                  step="0.1"
                  className="w-44"
                  value={multiplier}
                  onChange={(e) => setMultiplier(e.target.value)}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="normative-stale">Застій без активності, днів</Label>
                <Input
                  id="normative-stale"
                  type="number"
                  inputMode="numeric"
                  min={MIN_STALE_DAYS}
                  max={MAX_STALE_DAYS}
                  step="1"
                  className="w-44"
                  value={staleDays}
                  onChange={(e) => setStaleDays(e.target.value)}
                />
              </div>
              <Button onClick={() => void handleSave()} disabled={saving}>
                {saving ? 'Збереження…' : 'Зберегти'}
              </Button>
            </div>
            {formError && (
              <p className="mt-2 text-sm text-destructive" role="alert">
                {formError}
              </p>
            )}
            <p className="mt-2 text-xs text-muted-foreground">
              OVERDUE — календарний час перевищує норму в K разів; STALE — відкритий
              виріб без активності понад N днів. Зміни діють одразу та записуються в аудит.
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
