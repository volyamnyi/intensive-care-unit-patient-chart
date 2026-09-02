import { useCallback, useEffect, useRef, useState } from 'react';
import { toast } from 'sonner';
import { Download, Trash2, Upload } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { flowInstanceApi } from '@/api/prosthetics';
import { getErrorMessage } from '@/utils/errorMessage';
import type { EvidenceFile } from '@/prosthetics/types';

interface Props {
  instanceId: string;
  executionId: string;
}

export default function StepNoteAttachments({ instanceId, executionId }: Props) {
  const [note, setNote] = useState('');
  const [initialNote, setInitialNote] = useState('');
  const [files, setFiles] = useState<EvidenceFile[]>([]);
  const [noteSaving, setNoteSaving] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const load = useCallback(async () => {
    try {
      const [execRes, filesRes] = await Promise.all([
        flowInstanceApi.listExecutions(instanceId),
        flowInstanceApi.listEvidence(instanceId, executionId).catch(() => ({ data: [] as EvidenceFile[] })),
      ]);
      const exec = (execRes.data as unknown as Array<{ id: string; note?: string | null }>).find(
        (e) => e.id === executionId,
      );
      const n = exec?.note ?? '';
      setNote(n);
      setInitialNote(n);
      setFiles((filesRes.data as EvidenceFile[]) ?? []);
    } catch {
      // silent — not critical for step completion
    }
  }, [instanceId, executionId]);

  useEffect(() => {
    load();
  }, [load]);

  const saveNote = async () => {
    if (note === initialNote) return;
    if (note.length > 2000) {
      toast.error('Примітка не повинна перевищувати 2000 символів');
      return;
    }
    setNoteSaving(true);
    try {
      await flowInstanceApi.patchStepNote(instanceId, executionId, { note: note.trim() ? note : null });
      setInitialNote(note);
      toast.success('Примітку збережено');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося зберегти примітку'));
    } finally {
      setNoteSaving(false);
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (files.length >= 10) {
      toast.error('Досягнуто ліміт файлів на кроці (10)');
      e.target.value = '';
      return;
    }
    try {
      await flowInstanceApi.uploadEvidence(instanceId, executionId, file);
      toast.success('Файл завантажено');
      await load();
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося завантажити файл'));
    } finally {
      e.target.value = '';
    }
  };

  const handleDelete = async (fileId: string) => {
    try {
      await flowInstanceApi.deleteEvidence(instanceId, fileId);
      toast.success('Файл видалено');
      setFiles((prev) => prev.filter((f) => f.id !== fileId));
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося видалити файл'));
    }
  };

  const handleDownload = async (file: EvidenceFile) => {
    try {
      const res = await flowInstanceApi.downloadEvidence(instanceId, file.id);
      const blob = res.data as unknown as Blob;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = file.fileName;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      toast.error(getErrorMessage(err, 'Не вдалося завантажити файл'));
    }
  };

  return (
    <div className="space-y-4 rounded-lg border p-4">
      <div className="space-y-2">
        <Label htmlFor={`step-note-${executionId}`} className="text-sm">
          Примітка
        </Label>
        <Textarea
          id={`step-note-${executionId}`}
          rows={3}
          maxLength={2000}
          placeholder="Введіть примітку..."
          value={note}
          onChange={(ev) => setNote(ev.target.value)}
          onBlur={() => void saveNote()}
        />
        <div className="flex items-center gap-2">
          <span className="text-xs text-muted-foreground">{note.length}/2000</span>
          {note !== initialNote && (
            <Button
              size="sm"
              variant="outline"
              disabled={noteSaving}
              onClick={() => void saveNote()}
              className="ml-auto"
            >
              Зберегти
            </Button>
          )}
        </div>
      </div>

      <div className="space-y-2">
        <Label className="text-sm">Файли (до 10, зображення або PDF, ≤10 MB)</Label>
        <div
          className={`flex w-full items-center justify-center gap-2 rounded-md border border-dashed p-4 text-sm text-muted-foreground ${files.length >= 10 ? 'opacity-50' : ''}`}
        >
          <label className={`flex w-full cursor-pointer items-center justify-center gap-2 ${files.length >= 10 ? 'pointer-events-none' : ''}`}>
            <Input
              ref={fileInputRef}
              type="file"
              className="hidden"
              accept="image/*,application/pdf"
              onChange={handleFileChange}
              disabled={files.length >= 10}
            />
            <span className="flex items-center gap-2">
              <Upload className="size-4" />
              {files.length >= 10 ? 'Ліміт досягнуто' : 'Додати файл'}
            </span>
          </label>
        </div>
        {files.length > 0 && (
          <ul className="space-y-1">
            {files.map((f) => (
              <li
                key={f.id}
                className="flex items-center gap-2 rounded-md border px-3 py-2 text-sm"
              >
                <span className="min-w-0 flex-1 truncate" title={f.fileName}>
                  {f.fileName} ({f.mimeType}, {(f.sizeBytes / 1024).toFixed(1)} KB)
                </span>
                <Button size="icon-xs" variant="ghost" aria-label="Завантажити" onClick={() => void handleDownload(f)}>
                  <Download className="size-3.5" />
                </Button>
                <Button
                  size="icon-xs"
                  variant="ghost"
                  aria-label="Видалити"
                  className="text-destructive hover:bg-destructive/10"
                  onClick={() => void handleDelete(f.id)}
                >
                  <Trash2 className="size-3.5" />
                </Button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
