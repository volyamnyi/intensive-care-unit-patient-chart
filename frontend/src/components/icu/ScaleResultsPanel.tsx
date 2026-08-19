import { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { cn } from '@/lib/utils';
import { useThemeMode } from '../../styles/ThemeContext';
import ScaleFormFactory from './scales/ScaleFormFactory';
import type { ScaleResult, ClinicalScale } from '../../types/icu';

interface ScaleResultsPanelProps {
  results: ScaleResult[];
  availableScales: ClinicalScale[];
  onCreateResult?: (scaleId: string, result: string) => void;
  onCalculateScale?: (scaleId: string, rawData: Record<string, unknown>) => void;
  disabled?: boolean;
  episodeId?: string;
}

export default function ScaleResultsPanel({ results, availableScales, onCreateResult, onCalculateScale, disabled }: ScaleResultsPanelProps) {
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

  const selectedScale = availableScales.find(s => s.id === selectedScaleId);

  return (
    <>
      {(onCreateResult || onCalculateScale) && availableScales.length > 0 && (
        <div className="mb-2">
          <div className="flex flex-wrap items-start gap-2 mb-2">
            <Select value={selectedScaleId} onValueChange={(v: string | null) => { if (v !== null) setSelectedScaleId(v); }}>
              <SelectTrigger aria-label="Шкала" className="h-7 w-full sm:w-[200px] pointer-coarse:min-h-11">
                <SelectValue placeholder="Шкала" />
              </SelectTrigger>
              <SelectContent>
                {availableScales.map((s) => (
                  <SelectItem key={s.id} value={s.id}>{s.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            {selectedScale && !selectedScale.isAutomatic && !onCalculateScale && (
              <>
                <input
                  placeholder="Результат"
                  value={resultValue}
                  onChange={(e) => setResultValue(e.target.value)}
                  className="flex h-7 w-full sm:w-[120px] rounded-md border border-input bg-transparent px-2.5 py-1 text-xs transition-colors outline-none file:inline-flex file:h-6 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring disabled:pointer-events-none disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50 md:text-sm pointer-coarse:min-h-11"
                />
                <Button size="sm" className="min-h-11" onClick={handleCreate}>Додати</Button>
              </>
            )}
          </div>
          {selectedScale && onCalculateScale && !selectedScale.isAutomatic && (
            <div className="mb-2 p-2 rounded-lg border border-border">
              <ScaleFormFactory
                scaleName={selectedScale.name}
                onCalculate={(rawData) => onCalculateScale(selectedScale.id, rawData)}
                disabled={disabled}
              />
            </div>
          )}
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
