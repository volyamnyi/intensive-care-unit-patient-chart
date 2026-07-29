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
    <div
      className="fixed z-50"
      style={{
        top: deleteAnchor.getBoundingClientRect().bottom + 4,
        left: deleteAnchor.getBoundingClientRect().left,
      }}
    >
      <div className="rounded-xl border bg-popover text-popover-foreground shadow-md p-2 min-w-[220px]">
        <p className="text-sm mb-1.5">
          Видалити препарат? Дані про дози будуть втрачені.
        </p>
        <div className="flex gap-1 justify-end">
          <Button size="sm" variant="outline" onClick={onCloseDeleteConfirm} disabled={deleting}>Скасувати</Button>
          <Button size="sm" variant="destructive"
            disabled={deleting} onClick={onConfirmDelete}>Видалити</Button>
        </div>
      </div>
    </div>
  );
}
