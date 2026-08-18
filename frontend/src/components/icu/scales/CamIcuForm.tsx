import { useState, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';

interface CamIcuFormProps {
  onCalculate: (rawData: Record<string, unknown>) => void;
  disabled?: boolean;
}

export default function CamIcuForm({ onCalculate, disabled }: CamIcuFormProps) {
  const [acuteOnset, setAcuteOnset] = useState(false);
  const [inattention, setInattention] = useState(false);
  const [disorganizedThinking, setDisorganizedThinking] = useState(false);
  const [alteredConsciousness, setAlteredConsciousness] = useState(false);

  const delirium = useMemo(() => {
    return acuteOnset && inattention && (disorganizedThinking || alteredConsciousness);
  }, [acuteOnset, inattention, disorganizedThinking, alteredConsciousness]);

  const handleCalculate = () => {
    onCalculate({
      acuteOnset,
      inattention,
      disorganizedThinking,
      alteredConsciousness,
    });
  };

  return (
    <div className="flex flex-col gap-2">
      <p className="text-xs font-semibold mb-1">CAM-ICU — оцінка делірію</p>
      <div className="flex flex-col gap-1.5">
        <label className="flex items-center gap-2 text-xs">
          <Checkbox checked={acuteOnset} onCheckedChange={v => setAcuteOnset(v === true)} disabled={disabled} />
          Гострий початок або флуктуюючий перебіг
        </label>
        <label className="flex items-center gap-2 text-xs">
          <Checkbox checked={inattention} onCheckedChange={v => setInattention(v === true)} disabled={disabled} />
          Нездатність утримувати увагу
        </label>
        <label className="flex items-center gap-2 text-xs">
          <Checkbox checked={disorganizedThinking} onCheckedChange={v => setDisorganizedThinking(v === true)} disabled={disabled} />
          Дезорганізоване мислення
        </label>
        <label className="flex items-center gap-2 text-xs">
          <Checkbox checked={alteredConsciousness} onCheckedChange={v => setAlteredConsciousness(v === true)} disabled={disabled} />
          Змінений рівень свідомості (RASS ≠ 0)
        </label>
      </div>
      <div className="text-xs font-semibold">
        {delirium ? 'Делірій: ПОЗИТИВНИЙ' : 'Делірій: НЕГАТИВНИЙ'}
      </div>
      <div>
        <Button size="sm" onClick={handleCalculate} disabled={disabled}>
          Зберегти CAM-ICU
        </Button>
      </div>
    </div>
  );
}
