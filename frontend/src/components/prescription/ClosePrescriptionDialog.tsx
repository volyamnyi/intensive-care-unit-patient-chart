import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { TriangleAlert } from 'lucide-react';

interface ClosePrescriptionDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  allCompleted: boolean;
  closing?: boolean;
}

export default function ClosePrescriptionDialog({ open, onClose, onConfirm, allCompleted, closing }: ClosePrescriptionDialogProps) {
  return (
    <Dialog open={open} onOpenChange={(open) => { if (!open) onClose(); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="font-rubik">Закрити листок призначень</DialogTitle>
        </DialogHeader>
        {!allCompleted && (
          <Alert variant="default" className="mb-2 border-amber-500/30 bg-amber-50 dark:bg-amber-950/20">
            <TriangleAlert className="size-4" />
            <AlertDescription>
              Не всі частини доби виконані. Переконайтеся, що всі дози виконані перед закриттям.
            </AlertDescription>
          </Alert>
        )}
        <p>
          Після закриття листок призначень більше не можна редагувати. Підтвердити?
        </p>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={closing}>Скасувати</Button>
          <Button onClick={onConfirm} disabled={closing}>
            {closing ? 'Закриття...' : 'Закрити'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
