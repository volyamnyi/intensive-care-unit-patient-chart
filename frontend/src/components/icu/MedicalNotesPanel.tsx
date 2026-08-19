import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { useThemeMode } from '../../styles/ThemeContext';
import type { MedicalNote } from '../../types/icu';

interface MedicalNotesPanelProps {
  notes: MedicalNote[];
  onCreateNote?: (text: string, noteType: string) => void;
}

export default function MedicalNotesPanel({ notes, onCreateNote }: MedicalNotesPanelProps) {
  const { mode } = useThemeMode();
  const isDark = mode === 'dark';
  const [newText, setNewText] = useState('');

  const handleCreate = () => {
    if (!onCreateNote || !newText.trim()) return;
    onCreateNote(newText.trim(), 'DOCTOR_NOTE');
    setNewText('');
  };

  return (
    <>
      {onCreateNote && (
        <div className="mb-2">
          <textarea
            className="flex min-h-[72px] w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-base transition-colors outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring disabled:pointer-events-none disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50 md:text-sm dark:bg-input/30 dark:disabled:bg-input/80"
            placeholder="Нова нотатка"
            value={newText}
            onChange={(e) => setNewText(e.target.value)}
            rows={3}
          />
          <Button variant="default" className="mt-1" onClick={handleCreate}>
            {'Додати'}
          </Button>
        </div>
      )}

      {notes.length === 0 ? (
        <p className="text-muted-foreground">{'Немає нотаток'}</p>
      ) : (
        notes.map((n) => (
          <div
            key={n.id}
            className="rounded-xl border bg-card text-card-foreground shadow-sm p-2 mb-1"
            style={{
              borderColor: isDark ? '#2A2A2A' : '#E8E6E1',
              boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 8px rgba(0,0,0,0.04)',
            }}
          >
            <p className="text-sm text-muted-foreground">
              {n.authorId} &middot; {new Date(n.createdAt).toLocaleString('uk-UA')} &middot; {n.role}
            </p>
            <p className="mt-0.5 whitespace-pre-wrap">{n.text}</p>
          </div>
        ))
      )}
    </>
  );
}
