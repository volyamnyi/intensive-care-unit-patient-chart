export const CLINICAL_RANGES = {
  temperature: { min: 34, max: 42, unit: '°C', label: 'Температура' },
  heartRate: { min: 0, max: 300, unit: 'уд/хв', label: 'ЧСС' },
  respiratoryRate: { min: 0, max: 60, unit: '/хв', label: 'ЧД' },
  systolicBP: { min: 50, max: 250, unit: 'мм рт.ст.', label: 'АТ сист.' },
  diastolicBP: { min: 30, max: 150, unit: 'мм рт.ст.', label: 'АТ діаст.' },
  spo2: { min: 50, max: 100, unit: '%', label: 'SpO2' },
} as const;
