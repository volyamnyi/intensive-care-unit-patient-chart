import { Popover as PopoverPrimitive } from '@base-ui/react/popover';
import { Button } from '@/components/ui/button';

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
    <PopoverPrimitive.Root
      open
      onOpenChange={(open: boolean) => {
        if (!open) onCloseDeleteConfirm();
      }}
    >
      <PopoverPrimitive.Portal>
        <PopoverPrimitive.Positioner anchor={deleteAnchor} align="start" sideOffset={4}>
          <PopoverPrimitive.Popup
            data-slot="popover-content"
            className="z-50 rounded-xl border border-border bg-popover p-2 text-sm text-popover-foreground shadow-md min-w-[220px] outline-none data-open:animate-in data-open:fade-in-0 data-open:zoom-in-95 data-closed:animate-out data-closed:fade-out-0"
          >
            <p className="mb-1.5">
              Видалити препарат? Дані про дози будуть втрачені.
            </p>
            <div className="flex justify-end gap-1">
              <Button size="sm" variant="outline" onClick={onCloseDeleteConfirm} disabled={deleting}>Скасувати</Button>
              <Button size="sm" variant="destructive"
                disabled={deleting} onClick={onConfirmDelete}>Видалити</Button>
            </div>
          </PopoverPrimitive.Popup>
        </PopoverPrimitive.Positioner>
      </PopoverPrimitive.Portal>
    </PopoverPrimitive.Root>
  );
}
