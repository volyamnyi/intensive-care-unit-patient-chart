import { useState } from 'react';
import {
  Box, Paper, Typography, Grid, TextField, Button, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Chip, IconButton,
} from '@mui/material';
import { CheckCircle } from '@mui/icons-material';
import type { MedicalOrder, MedicalOrderCreateRequest } from '../../types';

interface MedicalOrdersPanelProps {
  orders: MedicalOrder[];
  onCreateOrder?: (order: MedicalOrderCreateRequest) => void;
  onExecuteOrder?: (orderId: string) => void;
  onCancelOrder?: (orderId: string) => void;
  canCreate?: boolean;
  canExecute?: boolean;
}

const statusLabels: Record<string, string> = {
  DRAFT: 'Чернетка',
  ACTIVE: 'Активне',
  COMPLETED: 'Виконано',
  CANCELLED: 'Скасовано',
};

const emptyOrder: MedicalOrderCreateRequest = {
  category: 'MEDICATION',
  drugName: '',
  dose: '',
  unit: '',
  route: '',
  frequency: '',
  startTime: '',
  endTime: '',
};

export default function MedicalOrdersPanel({
  orders, onCreateOrder, onExecuteOrder, onCancelOrder: _onCancelOrder, canCreate, canExecute,
}: MedicalOrdersPanelProps) {
  const [newOrder, setNewOrder] = useState<MedicalOrderCreateRequest>(emptyOrder);
  const [showForm, setShowForm] = useState(false);

  const handleCreate = () => {
    if (!onCreateOrder || !newOrder.drugName || !newOrder.dose) return;
    onCreateOrder(newOrder);
    setNewOrder(emptyOrder);
    setShowForm(false);
  };

  return (
    <>
      {canCreate && (
        <Box sx={{ mb: 2 }}>
          {showForm ? (
            <Paper sx={{ p: 2, mb: 2, border: '1px solid #E8E6E1' }}>
              <Typography variant="subtitle1" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1 }}>
                Нове призначення
              </Typography>
              <Grid container spacing={1} sx={{ alignItems: 'center' }}>
                <Grid size={3}>
                  <TextField fullWidth size="small" label="Категорія"
                    value={newOrder.category}
                    onChange={(e) => setNewOrder({ ...newOrder, category: e.target.value })} />
                </Grid>
                <Grid size={3}>
                  <TextField fullWidth size="small" label="Препарат"
                    value={newOrder.drugName}
                    onChange={(e) => setNewOrder({ ...newOrder, drugName: e.target.value })} />
                </Grid>
                <Grid size={2}>
                  <TextField fullWidth size="small" label="Доза"
                    value={newOrder.dose}
                    onChange={(e) => setNewOrder({ ...newOrder, dose: e.target.value })} />
                </Grid>
                <Grid size={2}>
                  <TextField fullWidth size="small" label="Од."
                    value={newOrder.unit}
                    onChange={(e) => setNewOrder({ ...newOrder, unit: e.target.value })} />
                </Grid>
                <Grid size={2}>
                  <TextField fullWidth size="small" label="Шлях"
                    value={newOrder.route}
                    onChange={(e) => setNewOrder({ ...newOrder, route: e.target.value })} />
                </Grid>
                <Grid size={3}>
                  <TextField fullWidth size="small" label="Частота"
                    value={newOrder.frequency}
                    onChange={(e) => setNewOrder({ ...newOrder, frequency: e.target.value })} />
                </Grid>
                <Grid size={3}>
                  <TextField fullWidth size="small" type="datetime-local" label="Початок"
                    value={newOrder.startTime}
                    onChange={(e) => setNewOrder({ ...newOrder, startTime: e.target.value })}
                    slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={3}>
                  <TextField fullWidth size="small" type="datetime-local" label="Кінець"
                    value={newOrder.endTime}
                    onChange={(e) => setNewOrder({ ...newOrder, endTime: e.target.value })}
                    slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={3}>
                  <Button variant="contained" size="small" onClick={handleCreate}>
                    Створити
                  </Button>
                  <Button size="small" sx={{ ml: 1 }} onClick={() => setShowForm(false)}>
                    Скасувати
                  </Button>
                </Grid>
              </Grid>
            </Paper>
          ) : (
            <Button variant="outlined" size="small" onClick={() => setShowForm(true)}>
              + Нове призначення
            </Button>
          )}
        </Box>
      )}

      <TableContainer component={Paper} sx={{ border: '1px solid #E8E6E1', boxShadow: '0 2px 12px rgba(0,0,0,0.04)' }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Препарат</TableCell>
              <TableCell>Доза</TableCell>
              <TableCell>Шлях</TableCell>
              <TableCell>Статус</TableCell>
              {canExecute && <TableCell>Виконання</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {orders.length === 0 ? (
              <TableRow>
                <TableCell colSpan={canExecute ? 5 : 4} align="center" sx={{ py: 3, color: '#5A5A5A' }}>
                  Немає призначень
                </TableCell>
              </TableRow>
            ) : (
              orders.map((order) => (
                <TableRow key={order.id}>
                  <TableCell sx={{ fontWeight: 600 }}>{order.drugName}</TableCell>
                  <TableCell>{order.dose} {order.unit}</TableCell>
                  <TableCell>{order.route}</TableCell>
                  <TableCell>
                    <Chip
                      label={statusLabels[order.status] || order.status}
                      size="small"
                      color={order.status === 'ACTIVE' ? 'success' : order.status === 'CANCELLED' ? 'default' : 'info'}
                    />
                  </TableCell>
                  {canExecute && (
                    <TableCell>
                      {order.status === 'ACTIVE' && onExecuteOrder && (
                        <IconButton
                          onClick={() => onExecuteOrder(order.id)}
                          sx={{ color: '#1F6B4C', '&:hover': { bgcolor: '#F0F7F3' } }}
                        >
                          <CheckCircle />
                        </IconButton>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}
