import { useTranslation } from 'react-i18next';
import { Dialog, DialogTitle, DialogContent, Typography, DialogActions, Button } from '@mui/material';

interface SignDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  dayNumber: number;
  signing?: boolean;
  role?: string;
}

export default function SignDialog({ open, onClose, onConfirm, dayNumber, signing, role }: SignDialogProps) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle sx={{ fontFamily: '"Rubik", sans-serif' }}>
        {t('signDialog.title', { dayNumber })}
      </DialogTitle>
      <DialogContent>
        <Typography>
          {t('signDialog.text', { role: t(role === 'NURSE' ? 'signDialog.roleNurse' : 'signDialog.roleDoctor') })}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          {role === 'NURSE'
            ? t('signDialog.nurseInfo')
            : t('signDialog.doctorInfo')}
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('signDialog.cancelButton')}</Button>
        <Button
          variant="contained" onClick={onConfirm} disabled={signing}
        >
          {signing ? t('signDialog.signingButton') : t('signDialog.signButton')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
