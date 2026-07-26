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
  const roleLabel = role === 'NURSE' ? 'медсестра' : 'лікар';
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle sx={{ fontFamily: '"Rubik", sans-serif' }}>
        {`Підписання дня ${dayNumber}`}
      </DialogTitle>
      <DialogContent>
        <Typography>
          {`Підтвердьте підписання клінічного дня як ${roleLabel}`}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          {role === 'NURSE'
            ? 'Після підписання медсестрою день буде доступний для підписання лікарем.'
            : 'Після підписання лікарем клінічний день буде закрито.'}
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{'Скасувати'}</Button>
        <Button
          variant="contained" onClick={onConfirm} disabled={signing}
        >
          {signing ? 'Підписання...' : 'Підписати'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
