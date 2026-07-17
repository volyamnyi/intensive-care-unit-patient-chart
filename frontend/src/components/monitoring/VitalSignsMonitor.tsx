import { useTheme } from '@mui/material';
import type { HourlyRecord } from '../../types';

interface VitalSignsMonitorProps {
  records: HourlyRecord[];
  currentHour: number;
}

interface VitalDisplay {
  key: string;
  label: string;
  unit: string;
  value: string;
  abnormal: 'normal' | 'warning' | 'critical';
  subLabel?: string;
}

function getHRStatus(v: number | null): 'normal' | 'warning' | 'critical' {
  if (v === null || v === undefined) return 'normal';
  if (v < 40 || v > 140) return 'critical';
  if (v < 60 || v > 100) return 'warning';
  return 'normal';
}

function getBpStatus(sys: number | null, dia: number | null): 'normal' | 'warning' | 'critical' {
  if (sys === null || sys === undefined || dia === null || dia === undefined) return 'normal';
  if (sys < 70 || sys > 200 || dia < 40 || dia > 120) return 'critical';
  if (sys < 90 || sys > 160 || dia < 60 || dia > 100) return 'warning';
  return 'normal';
}

function getSpo2Status(v: number | null): 'normal' | 'warning' | 'critical' {
  if (v === null || v === undefined) return 'normal';
  if (v < 85) return 'critical';
  if (v < 95) return 'warning';
  return 'normal';
}

function getTempStatus(v: number | null): 'normal' | 'warning' | 'critical' {
  if (v === null || v === undefined) return 'normal';
  if (v < 35 || v > 40) return 'critical';
  if (v < 36 || v > 38) return 'warning';
  return 'normal';
}

function getRRStatus(v: number | null): 'normal' | 'warning' | 'critical' {
  if (v === null || v === undefined) return 'normal';
  if (v < 8 || v > 30) return 'critical';
  if (v < 12 || v > 20) return 'warning';
  return 'normal';
}

function getCVPStatus(v: number | null): 'normal' | 'warning' | 'critical' {
  if (v === null || v === undefined) return 'normal';
  if (v < 0 || v > 18) return 'critical';
  if (v < 2 || v > 12) return 'warning';
  return 'normal';
}

function formatMap(dia: number | null, sys: number | null): string | null {
  if (dia === null || dia === undefined || sys === null || sys === undefined) return null;
  return String(Math.round(dia + (sys - dia) / 3));
}

function statusColor(s: 'normal' | 'warning' | 'critical', theme: ReturnType<typeof useTheme>): string {
  if (s === 'critical') return '#FF1744';
  if (s === 'warning') return '#FF9100';
  return theme.palette.mode === 'dark' ? '#4CAF50' : '#2E7D32';
}

function statusBg(s: 'normal' | 'warning' | 'critical', isDark: boolean): string {
  if (s === 'critical') return isDark ? '#3A0A0A' : '#FFEBEE';
  if (s === 'warning') return isDark ? '#3A2A00' : '#FFF3E0';
  return isDark ? '#0A2A1A' : '#E8F5E9';
}

export default function VitalSignsMonitor({ records, currentHour }: VitalSignsMonitorProps) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const current = records.find(r => {
    const h = Number(String(r.recordTime).substring(11, 13));
    return h === currentHour;
  });

  const mapVal = formatMap(current?.diastolicBP ?? null, current?.systolicBP ?? null);

  const vitals: VitalDisplay[] = [
    { key: 'hr', label: 'HR', unit: 'bpm', value: current?.heartRate?.toString() ?? '--',
      abnormal: getHRStatus(current?.heartRate ?? null) },
    { key: 'bp', label: 'BP', unit: 'mmHg', value: `${current?.systolicBP ?? '--'}/${current?.diastolicBP ?? '--'}`,
      abnormal: getBpStatus(current?.systolicBP ?? null, current?.diastolicBP ?? null),
      subLabel: mapVal ? `MAP ${mapVal}` : undefined },
    { key: 'spo2', label: 'SpO₂', unit: '%', value: current?.spo2?.toString() ?? '--',
      abnormal: getSpo2Status(current?.spo2 ?? null) },
    { key: 'temp', label: 'Temp', unit: '°C', value: current?.temperature?.toFixed(1) ?? '--',
      abnormal: getTempStatus(current?.temperature ?? null) },
    { key: 'rr', label: 'RR', unit: '/min', value: current?.respiratoryRate?.toString() ?? '--',
      abnormal: getRRStatus(current?.respiratoryRate ?? null) },
    { key: 'cvp', label: 'CVP', unit: 'mmHg', value: current?.cvp?.toString() ?? '--',
      abnormal: getCVPStatus(current?.cvp ?? null) },
  ];

  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
      {vitals.map(v => {
        const sc = statusColor(v.abnormal, theme);
        const sbg = statusBg(v.abnormal, isDark);
        const isCritical = v.abnormal === 'critical';
        return (
          <div key={v.key} style={{
            flex: '1 0 100px',
            minWidth: 110,
            maxWidth: 170,
            background: sbg,
            borderRadius: 12,
            padding: '12px 10px',
            textAlign: 'center',
            border: `2px solid ${sc}`,
            boxShadow: isCritical ? `0 0 16px ${sc}44` : 'none',
            transition: 'all 0.3s ease',
          }}>
            <div style={{ fontSize: 10, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.08em',
              color: theme.palette.text.secondary, marginBottom: 2 }}>
              {v.label}
            </div>
            <div style={{ fontSize: isCritical ? 26 : 22, fontWeight: 800, fontFamily: '"Rubik", sans-serif',
              lineHeight: 1.1, color: sc, letterSpacing: '-0.02em' }}>
              {v.value}
            </div>
            <div style={{ fontSize: 10, color: theme.palette.text.secondary, marginTop: 1 }}>
              {v.unit}
            </div>
            {v.subLabel && (
              <div style={{ fontSize: 11, fontWeight: 600, color: theme.palette.text.secondary, marginTop: 2 }}>
                {v.subLabel}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
