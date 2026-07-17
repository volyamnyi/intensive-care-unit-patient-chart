import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Paper, Typography, TextField, Button, useTheme } from '@mui/material';
import type { MedicalNote } from '../../types';

interface MedicalNotesPanelProps {
  notes: MedicalNote[];
  onCreateNote?: (text: string, noteType: string) => void;
}

export default function MedicalNotesPanel({ notes, onCreateNote }: MedicalNotesPanelProps) {
  const theme = useTheme();
  const { t } = useTranslation();
  const isDark = theme.palette.mode === 'dark';
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
            fullWidth multiline minRows={3} label={t('medicalNotes.newNoteLabel')}
            value={newText}
            onChange={(e) => setNewText(e.target.value)}
          />
          <Button variant="contained" sx={{ mt: 1 }} onClick={handleCreate}>
            {t('medicalNotes.addNoteButton')}
          </Button>
        </Box>
      )}

      {notes.length === 0 ? (
        <Typography color="text.secondary">{t('medicalNotes.empty')}</Typography>
      ) : (
        notes.map((n) => (
          <Paper key={n.id} sx={{ p: 2, mb: 1, border: `1px solid ${isDark ? '#2A2A2A' : '#E8E6E1'}`, boxShadow: isDark ? '0 2px 12px rgba(0,0,0,0.2)' : '0 2px 8px rgba(0,0,0,0.04)' }}>
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
