import { useState } from 'react';
import { Box, Paper, Typography, TextField, Button } from '@mui/material';
import type { MedicalNote } from '../../types';

interface MedicalNotesPanelProps {
  notes: MedicalNote[];
  onCreateNote?: (text: string, noteType: string) => void;
}

export default function MedicalNotesPanel({ notes, onCreateNote }: MedicalNotesPanelProps) {
  const [newText, setNewText] = useState('');

  const handleCreate = () => {
    if (!onCreateNote || !newText.trim()) return;
    onCreateNote(newText.trim(), 'DOCTOR_NOTE');
    setNewText('');
  };

  return (
    <>
      {onCreateNote && (
        <Box sx={{ mb: 2 }}>
          <TextField
            fullWidth multiline minRows={3} label="Нова нотатка"
            value={newText}
            onChange={(e) => setNewText(e.target.value)}
          />
          <Button variant="contained" sx={{ mt: 1 }} onClick={handleCreate}>
            Додати нотатку
          </Button>
        </Box>
      )}

      {notes.length === 0 ? (
        <Typography color="text.secondary">Немає нотаток</Typography>
      ) : (
        notes.map((n) => (
          <Paper key={n.id} sx={{ p: 2, mb: 1, border: '1px solid #2A2A2A', boxShadow: '0 2px 12px rgba(0,0,0,0.2)' }}>
            <Typography variant="body2" color="text.secondary">
              {n.authorId} &middot; {new Date(n.createdAt).toLocaleString('uk-UA')} &middot; {n.role}
            </Typography>
            <Typography sx={{ mt: 0.5, whiteSpace: 'pre-wrap' }}>{n.text}</Typography>
          </Paper>
        ))
      )}
    </>
  );
}
