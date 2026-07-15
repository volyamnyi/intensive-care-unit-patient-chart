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
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle sx={{ fontFamily: '"Rubik", sans-serif' }}>
        Підписання доби №{dayNumber}
      </DialogTitle>
      <DialogContent>
        <Typography>
          Після підписання доба стане read-only для {role === 'NURSE' ? 'медсестри' : 'лікаря'}.
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          {role === 'NURSE'
            ? 'Буде збережено підпис медсестри. Після підписання лікарем буде згенеровано PDF.'
            : 'Буде згенеровано PDF та відправлено в МІС.'}
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Скасувати</Button>
        <Button
          variant="contained" onClick={onConfirm} disabled={signing}
        >
          {signing ? 'Підписання...' : 'Підписати'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
