import i18n from '../i18n/i18n';

export const CLINICAL_RANGES = {
  temperature: { min: 34, max: 42, unit: i18n.t('clinicalRanges.units.celsius'), label: i18n.t('clinicalRanges.temperature') },
  heartRate: { min: 0, max: 300, unit: i18n.t('clinicalRanges.units.bpm'), label: i18n.t('clinicalRanges.heartRate') },
  respiratoryRate: { min: 0, max: 60, unit: i18n.t('clinicalRanges.units.perMin'), label: i18n.t('clinicalRanges.respiratoryRate') },
  systolicBP: { min: 50, max: 250, unit: i18n.t('clinicalRanges.units.mmhg'), label: i18n.t('clinicalRanges.systolicBP') },
  diastolicBP: { min: 30, max: 150, unit: i18n.t('clinicalRanges.units.mmhg'), label: i18n.t('clinicalRanges.diastolicBP') },
  spo2: { min: 50, max: 100, unit: i18n.t('clinicalRanges.units.percent'), label: i18n.t('clinicalRanges.spo2') },
} as const;
