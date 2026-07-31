import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

const rassOptions = [
  { value: '+4', label: '+4 — Агресивний' },
  { value: '+3', label: '+3 — Дуже збуджений' },
  { value: '+2', label: '+2 — Збуджений' },
  { value: '+1', label: '+1 — Неспокійний' },
  { value: '0', label: '0 — Неспаний, спокійний' },
  { value: '-1', label: '-1 — Сонливий' },
  { value: '-2', label: '-2 — Легка седація' },
  { value: '-3', label: '-3 — Помірна седація' },
  { value: '-4', label: '-4 — Глибока седація' },
  { value: '-5', label: '-5 — Неможливо розбудити' },
];

interface RassSelectorProps {
  value?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}

export default function RassSelector({ value, onChange, disabled }: RassSelectorProps) {
  return (
    <Select value={value} onValueChange={(v: string | null) => onChange(v ?? '')} disabled={disabled}>
      <SelectTrigger className="h-7 text-xs w-[200px]">
        <SelectValue placeholder="RASS" />
      </SelectTrigger>
      <SelectContent>
        {rassOptions.map(o => (
          <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
