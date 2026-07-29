import { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { cn } from '@/lib/utils';
import { useThemeMode } from '../../styles/ThemeContext';
import type { ScaleResult, ClinicalScale } from '../../types';

interface ScaleResultsPanelProps {
  results: ScaleResult[];
  availableScales: ClinicalScale[];
  onCreateResult?: (scaleId: string, result: string) => void;
}

export default function ScaleResultsPanel({ results, availableScales, onCreateResult }: ScaleResultsPanelProps) {
  const { mode } = useThemeMode();
  const isDark = mode === 'dark';
  const [selectedScaleId, setSelectedScaleId] = useState('');
  const [resultValue, setResultValue] = useState('');

  const handleCreate = () => {
    if (!onCreateResult || !selectedScaleId || !resultValue.trim()) return;
    onCreateResult(selectedScaleId, resultValue.trim());
    setSelectedScaleId('');
    setResultValue('');
  };

  const isAutoScale = (name: string) => /GCS|RASS|glasgow|richmond/i.test(name);

  const getResultForScale = (scaleId: string) => results.find((r) => r.scaleId === scaleId);

  return (
    <>
      {onCreateResult && availableScales.length > 0 && (
        <div className="mb-2 flex flex-wrap items-start gap-2">
          <Select value={selectedScaleId} onValueChange={(v: string | null) => { if (v !== null) setSelectedScaleId(v); }}>
            <SelectTrigger aria-label="Шкала" className="h-7 w-full sm:w-[200px]">
              <SelectValue placeholder="Шкала" />
            </SelectTrigger>
            <SelectContent>
              {availableScales.map((s) => (
                <SelectItem key={s.id} value={s.id}>{s.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Input
            placeholder="Результат"
            value={resultValue}
            onChange={(e) => setResultValue(e.target.value)}
            className="h-7 w-full sm:w-[120px]"
          />
          <Button size="sm" onClick={handleCreate}>Додати</Button>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {availableScales.length === 0 && results.length === 0 ? (
          <div className="col-span-full">
            <p className="text-muted-foreground">Немає результатів</p>
          </div>
        ) : (
          availableScales.map((scale) => {
            const result = getResultForScale(scale.id);
            return (
              <div key={scale.id} className={cn('rounded-xl border bg-card p-4 text-card-foreground shadow-sm', isDark ? 'border-[#2A2A2A] shadow-[0_2px_12px_rgba(0,0,0,0.2)]' : 'border-[#E8E6E1] shadow-[0_2px_8px_rgba(0,0,0,0.04)]')}>
                <p className="font-rubik text-sm font-semibold">
                  {scale.name}
                  {isAutoScale(scale.name) && (
                    <Badge variant="secondary" className="ml-1 h-[18px] text-[9px] font-bold">Auto</Badge>
                  )}
                </p>
                {result ? (
                  <p className="mt-0.5 text-sm font-mulish">
                    {`Результат: ${result.result} (${new Date(result.calculatedAt).toLocaleString('uk-UA')})`}
                  </p>
                ) : (
                  <p className="mt-0.5 text-sm text-muted-foreground font-mulish">Не заповнено</p>
                )}
              </div>
            );
          })
        )}
      </div>
    </>
  );
}
