import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';

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
    <Dialog open={open} onOpenChange={(open) => { if (!open) onClose(); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="font-rubik">{`Підписання дня ${dayNumber}`}</DialogTitle>
        </DialogHeader>
        <p>{`Підтвердьте підписання клінічного дня як ${roleLabel}`}</p>
        <p className="text-sm text-muted-foreground mt-1">
          {role === 'NURSE'
            ? 'Після підписання медсестрою день буде доступний для підписання лікарем.'
            : 'Після підписання лікарем клінічний день буде закрито.'}
        </p>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>{'Скасувати'}</Button>
          <Button onClick={onConfirm} disabled={signing}>
            {signing ? 'Підписання...' : 'Підписати'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
