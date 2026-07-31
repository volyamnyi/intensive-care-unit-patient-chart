import { useState, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

interface ApacheIiFormProps {
  onCalculate: (rawData: Record<string, unknown>) => void;
  disabled?: boolean;
}

export default function ApacheIiForm({ onCalculate, disabled }: ApacheIiFormProps) {
  const [temperatureC, setTemperatureC] = useState('');
  const [meanArterialPressure, setMeanArterialPressure] = useState('');
  const [heartRate, setHeartRate] = useState('');
  const [respiratoryRate, setRespiratoryRate] = useState('');
  const [fio2, setFio2] = useState('');
  const [paO2, setPaO2] = useState('');
  const [paCO2, setPaCO2] = useState('');
  const [ph, setPh] = useState('');
  const [serumHco3, setSerumHco3] = useState('');
  const [serumSodium, setSerumSodium] = useState('');
  const [serumPotassium, setSerumPotassium] = useState('');
  const [serumCreatinine, setSerumCreatinine] = useState('');
  const [acuteRenalFailure, setAcuteRenalFailure] = useState('false');
  const [hematocrit, setHematocrit] = useState('');
  const [whiteBloodCount, setWhiteBloodCount] = useState('');
  const [gcs, setGcs] = useState('');
  const [age, setAge] = useState('');
  const [chronicHealthType, setChronicHealthType] = useState('NONE');
  const [emergencySurgical, setEmergencySurgical] = useState('false');

  const canCalculate = useMemo(() => {
    return temperatureC !== '' || meanArterialPressure !== '' || heartRate !== ''
      || respiratoryRate !== '' || fio2 !== '' || paO2 !== '' || ph !== ''
      || serumSodium !== '' || serumPotassium !== '' || serumCreatinine !== ''
      || hematocrit !== '' || whiteBloodCount !== '' || gcs !== '' || age !== '';
  }, [temperatureC, meanArterialPressure, heartRate, respiratoryRate, fio2, paO2, ph,
      serumSodium, serumPotassium, serumCreatinine, hematocrit, whiteBloodCount, gcs, age]);

  const handleCalculate = () => {
    const rawData: Record<string, unknown> = {};
    if (temperatureC !== '') rawData.temperatureC = parseFloat(temperatureC);
    if (meanArterialPressure !== '') rawData.meanArterialPressure = parseFloat(meanArterialPressure);
    if (heartRate !== '') rawData.heartRate = parseFloat(heartRate);
    if (respiratoryRate !== '') rawData.respiratoryRate = parseFloat(respiratoryRate);
    if (fio2 !== '') rawData.fio2 = parseFloat(fio2);
    if (paO2 !== '') rawData.paO2 = parseFloat(paO2);
    if (paCO2 !== '') rawData.paCO2 = parseFloat(paCO2);
    if (ph !== '') rawData.ph = parseFloat(ph);
    if (serumHco3 !== '') rawData.serumHco3 = parseFloat(serumHco3);
    if (serumSodium !== '') rawData.serumSodium = parseFloat(serumSodium);
    if (serumPotassium !== '') rawData.serumPotassium = parseFloat(serumPotassium);
    if (serumCreatinine !== '') rawData.serumCreatinine = parseFloat(serumCreatinine);
    rawData.acuteRenalFailure = acuteRenalFailure === 'true';
    if (hematocrit !== '') rawData.hematocrit = parseFloat(hematocrit);
    if (whiteBloodCount !== '') rawData.whiteBloodCount = parseFloat(whiteBloodCount);
    if (gcs !== '') rawData.gcs = parseInt(gcs, 10);
    if (age !== '') rawData.age = parseInt(age, 10);
    rawData.chronicHealthType = chronicHealthType;
    rawData.emergencySurgical = emergencySurgical === 'true';
    onCalculate(rawData);
  };

  return (
    <div className="flex flex-col gap-2">
      <p className="text-xs font-semibold mb-1">APACHE II — параметри (найгірші за 24 год)</p>
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-1.5">
        <Input placeholder="Температура (°C)" value={temperatureC} onChange={e => setTemperatureC(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="Середній АТ (mmHg)" value={meanArterialPressure} onChange={e => setMeanArterialPressure(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="ЧСС (уд/хв)" value={heartRate} onChange={e => setHeartRate(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="ЧД (дих/хв)" value={respiratoryRate} onChange={e => setRespiratoryRate(e.target.value)} className="h-7 text-xs" disabled={disabled} />
         <Input placeholder="FiO₂ (%)" value={fio2} onChange={e => setFio2(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="PaO₂ (mmHg)" value={paO2} onChange={e => setPaO2(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="PaCO₂ (mmHg)" value={paCO2} onChange={e => setPaCO2(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="pH (крові)" value={ph} onChange={e => setPh(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="HCO₃⁻ (mmol/L)" value={serumHco3} onChange={e => setSerumHco3(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="Na⁺ (mmol/L)" value={serumSodium} onChange={e => setSerumSodium(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="K⁺ (mmol/L)" value={serumPotassium} onChange={e => setSerumPotassium(e.target.value)} className="h-7 text-xs" disabled={disabled} />
         <Input placeholder="Креатинін (мкмоль/л або мг/дл)" value={serumCreatinine} onChange={e => setSerumCreatinine(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Select value={acuteRenalFailure} onValueChange={v => setAcuteRenalFailure(v ?? '')} disabled={disabled}>
          <SelectTrigger className="h-7 text-xs"><SelectValue placeholder="ГНН?" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="false">Немає ГНН</SelectItem>
            <SelectItem value="true">ГНН</SelectItem>
          </SelectContent>
        </Select>
        <Input placeholder="Гематокрит (%)" value={hematocrit} onChange={e => setHematocrit(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="WBC (×10³/µL)" value={whiteBloodCount} onChange={e => setWhiteBloodCount(e.target.value)} className="h-7 text-xs" disabled={disabled} />
         <Input placeholder="GCS (3–15)" value={gcs} onChange={e => setGcs(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Input placeholder="Вік (роки)" value={age} onChange={e => setAge(e.target.value)} className="h-7 text-xs" disabled={disabled} />
        <Select value={chronicHealthType} onValueChange={v => setChronicHealthType(v ?? '')} disabled={disabled}>
          <SelectTrigger className="h-7 text-xs"><SelectValue placeholder="Хронічне здоров'я" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="NONE">Немає</SelectItem>
            <SelectItem value="CHRONIC">Хронічна недостатність</SelectItem>
          </SelectContent>
        </Select>
        <Select value={emergencySurgical} onValueChange={v => setEmergencySurgical(v ?? '')} disabled={disabled}>
          <SelectTrigger className="h-7 text-xs"><SelectValue placeholder="Тип операції" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="false">Не хірургічний / плановий</SelectItem>
            <SelectItem value="true">Екстрений хірургічний</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div className="mt-1">
        <Button size="sm" onClick={handleCalculate} disabled={!canCalculate || disabled}>
          Розрахувати APACHE II
        </Button>
      </div>
    </div>
  );
}
