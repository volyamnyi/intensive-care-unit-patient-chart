import { Button } from '@/components/ui/button';
import {
  Popover,
  PopoverContent,
  PopoverDescription,
  PopoverPortal,
  PopoverPositioner,
  PopoverTitle,
} from '@/components/ui/popover';

export interface DeleteConfirmPopoverProps {
  deleteAnchor: HTMLElement | null;
  deleting: boolean;
  onCloseDeleteConfirm: () => void;
  onConfirmDelete: () => void;
}

export default function DeleteConfirmPopover({
  deleteAnchor, deleting, onCloseDeleteConfirm, onConfirmDelete,
}: DeleteConfirmPopoverProps) {
  if (!deleteAnchor) return null;

  return (
    <Popover
      open
      onOpenChange={(open: boolean) => {
        if (!open) onCloseDeleteConfirm();
      }}
    >
      <PopoverPortal>
        <PopoverPositioner anchor={deleteAnchor} align="start" sideOffset={4}>
          <PopoverContent className="z-50 min-w-[220px] rounded-xl p-2">
            <PopoverTitle className="mb-1.5 font-normal">
              Видалити препарат?
            </PopoverTitle>
            <PopoverDescription className="mb-2 text-xs">
              Дані про дози будуть втрачені.
            </PopoverDescription>
            <div className="flex justify-end gap-1">
              <Button size="sm" variant="outline" onClick={onCloseDeleteConfirm} disabled={deleting}>Скасувати</Button>
              <Button size="sm" variant="destructive"
                disabled={deleting} onClick={onConfirmDelete}>Видалити</Button>
            </div>
          </PopoverContent>
        </PopoverPositioner>
      </PopoverPortal>
    </Popover>
  );
}
