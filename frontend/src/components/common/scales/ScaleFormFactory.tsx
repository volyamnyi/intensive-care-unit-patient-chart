import ApacheIiForm from './ApacheIiForm';
import SofaForm from './SofaForm';
import CamIcuForm from './CamIcuForm';
import BradenForm from './BradenForm';
import RassSelector from './RassSelector';

interface ScaleFormFactoryProps {
  scaleName: string;
  onCalculate: (rawData: Record<string, unknown>) => void;
  onRassChange?: (value: string) => void;
  rassValue?: string;
  disabled?: boolean;
}

export default function ScaleFormFactory({ scaleName, onCalculate, onRassChange, rassValue, disabled }: ScaleFormFactoryProps) {
  const name = scaleName.toLowerCase();

  if (name.includes('apache')) {
    return <ApacheIiForm onCalculate={onCalculate} disabled={disabled} />;
  }
  if (name.includes('sofa')) {
    return <SofaForm onCalculate={onCalculate} disabled={disabled} />;
  }
  if (name.includes('cam-icu') || scaleName === 'CAM-ICU' || name.includes('cam')) {
    return <CamIcuForm onCalculate={onCalculate} disabled={disabled} />;
  }
  if (name.includes('браден') || name.includes('braden')) {
    return <BradenForm onCalculate={onCalculate} disabled={disabled} />;
  }
  if (name.includes('rass') || name.includes('ричмонд')) {
    return (
      <div className="flex items-center gap-2">
        <span className="text-xs">RASS:</span>
        <RassSelector value={rassValue} onChange={v => onRassChange?.(v)} disabled={disabled} />
      </div>
    );
  }

  return null;
}
