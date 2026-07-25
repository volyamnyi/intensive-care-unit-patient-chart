import { Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography, Alert } from '@mui/material';

interface ClosePrescriptionDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  allCompleted: boolean;
  closing?: boolean;
}

export default function ClosePrescriptionDialog({ open, onClose, onConfirm, allCompleted, closing }: ClosePrescriptionDialogProps) {
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle sx={{ fontFamily: '"Rubik", sans-serif' }}>Закрити листок призначень</DialogTitle>
      <DialogContent>
        {!allCompleted && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            Не всі частини доби виконані. Переконайтеся, що всі дози виконані перед закриттям.
          </Alert>
        )}
        <Typography>
          Після закриття листок призначень більше не можна редагувати. Підтвердити?
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={closing}>Скасувати</Button>
        <Button variant="contained" onClick={onConfirm} disabled={closing}>
          {closing ? 'Закриття...' : 'Закрити'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
