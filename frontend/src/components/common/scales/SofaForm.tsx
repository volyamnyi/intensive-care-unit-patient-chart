import { useState, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

interface SofaFormProps {
  onCalculate: (rawData: Record<string, unknown>) => void;
  disabled?: boolean;
}

export default function SofaForm({ onCalculate, disabled }: SofaFormProps) {
  const [paO2, setPaO2] = useState('');
  const [fio2, setFio2] = useState('');
  const [onVentilator, setOnVentilator] = useState('false');
  const [platelets, setPlatelets] = useState('');
  const [bilirubin, setBilirubin] = useState('');
  const [map, setMap] = useState('');
  const [dopamine, setDopamine] = useState('');
  const [dobutamine, setDobutamine] = useState('');
  const [norepinephrine, setNorepinephrine] = useState('');
  const [epinephrine, setEpinephrine] = useState('');
  const [gcs, setGcs] = useState('');
  const [creatinine, setCreatinine] = useState('');
  const [urineOutput, setUrineOutput] = useState('');

  const canCalculate = useMemo(() => {
    return paO2 !== '' || fio2 !== '' || platelets !== '' || bilirubin !== ''
      || map !== '' || gcs !== '' || creatinine !== '';
  }, [paO2, fio2, platelets, bilirubin, map, gcs, creatinine]);

  const handleCalculate = () => {
    const rawData: Record<string, unknown> = {};
    if (paO2 !== '') rawData.paO2 = parseFloat(paO2);
    if (fio2 !== '') rawData.fio2 = parseFloat(fio2);
    rawData.onVentilator = onVentilator === 'true';
    if (platelets !== '') rawData.platelets = parseFloat(platelets);
    if (bilirubin !== '') rawData.bilirubin = parseFloat(bilirubin);
    if (map !== '') rawData.map = parseFloat(map);
    if (dopamine !== '') rawData.dopamine = parseFloat(dopamine);
    if (dobutamine !== '') rawData.dobutamine = parseFloat(dobutamine);
    if (norepinephrine !== '') rawData.norepinephrine = parseFloat(norepinephrine);
    if (epinephrine !== '') rawData.epinephrine = parseFloat(epinephrine);
    if (gcs !== '') rawData.gcs = parseInt(gcs, 10);
    if (creatinine !== '') rawData.creatinine = parseFloat(creatinine);
    if (urineOutput !== '') rawData.urineOutput = parseFloat(urineOutput);
    onCalculate(rawData);
  };

  return (
    <div className="flex flex-col gap-2">
      <p className="text-xs font-semibold mb-1">SOFA — параметри</p>
      <div className="grid grid-cols-2 md:grid-cols-3 gap-1.5">
        <Input placeholder="PaO₂ (mmHg)" value={paO2} onChange={e => setPaO2(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="FiO₂ (%)" value={fio2} onChange={e => setFio2(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Select value={onVentilator} onValueChange={v => setOnVentilator(v)} disabled={disabled}>
          <SelectTrigger className="h-7 text-xs"><SelectValue placeholder="ШВЛ?" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="false">Ні</SelectItem>
            <SelectItem value="true">Так</SelectItem>
          </SelectContent>
        </Select>
        <Input placeholder="Тромбоцити (×10⁹/л)" value={platelets} onChange={e => setPlatelets(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="Білірубін (мкмоль/л або мг/дл)" value={bilirubin} onChange={e => setBilirubin(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="MAP (mmHg)" value={map} onChange={e => setMap(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="Допамін (µg/kg/min)" value={dopamine} onChange={e => setDopamine(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="Добутамін (µg/kg/min)" value={dobutamine} onChange={e => setDobutamine(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="Норепінефрин (µg/kg/min)" value={norepinephrine} onChange={e => setNorepinephrine(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="Епінефрин (µg/kg/min)" value={epinephrine} onChange={e => setEpinephrine(e.target.value)} className="h-7 text-xs" disabled={disabled} />
         <Input placeholder="GCS (3–15)" value={gcs} onChange={e => setGcs(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="Креатинін (мкмоль/л або мг/дл)" value={creatinine} onChange={e => setCreatinine(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="Добовий діурез (mL)" value={urineOutput} onChange={e => setUrineOutput(e.target.value)} className="h-7 text-xs" disabled={disabled} />
      </div>
      <div className="mt-1">
        <Button size="sm" onClick={handleCalculate} disabled={!canCalculate || disabled}>
          Розрахувати SOFA
        </Button>
      </div>
    </div>
  );
}
