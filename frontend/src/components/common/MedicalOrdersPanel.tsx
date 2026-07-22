import { useState } from 'react';
import {
  Box, Paper, Typography, Grid, TextField, Button, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Chip, IconButton, useTheme,
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
  orders, onCreateOrder, onExecuteOrder, onCancelOrder, canCreate, canExecute,
}: MedicalOrdersPanelProps) {
  const theme = useTheme();
  const statusLabels: Record<string, string> = {
    DRAFT: 'Чернетка',
    ACTIVE: 'Активний',
    COMPLETED: 'Виконаний',
    CANCELLED: 'Скасований',
  };
  const [newOrder, setNewOrder] = useState<MedicalOrderCreateRequest>(emptyOrder);
  const [showForm, setShowForm] = useState(false);
  const showActions = canExecute || !!onCancelOrder;

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
            <Paper sx={{ p: 2, mb: 2 }}>
              <Typography variant="subtitle1" sx={{ fontFamily: '"Rubik", sans-serif', mb: 1 }}>
                {'Нове призначення'}
              </Typography>
              <Grid container spacing={1} sx={{ alignItems: 'center' }}>
                <Grid size={{ xs: 6, sm: 3 }}>
                  <TextField fullWidth size="small" label={'Категорія'}
                    value={newOrder.category}
                    onChange={(e) => setNewOrder({ ...newOrder, category: e.target.value })} />
                </Grid>
                <Grid size={{ xs: 6, sm: 3 }}>
                  <TextField fullWidth size="small" label={'Препарат'}
                    value={newOrder.drugName}
                    onChange={(e) => setNewOrder({ ...newOrder, drugName: e.target.value })} />
                </Grid>
                <Grid size={{ xs: 4, sm: 2 }}>
                  <TextField fullWidth size="small" label={'Доза'}
                    value={newOrder.dose}
                    onChange={(e) => setNewOrder({ ...newOrder, dose: e.target.value })} />
                </Grid>
                <Grid size={{ xs: 4, sm: 2 }}>
                  <TextField fullWidth size="small" label={'Од.'}
                    value={newOrder.unit}
                    onChange={(e) => setNewOrder({ ...newOrder, unit: e.target.value })} />
                </Grid>
                <Grid size={{ xs: 4, sm: 2 }}>
                  <TextField fullWidth size="small" label={'Шлях'}
                    value={newOrder.route}
                    onChange={(e) => setNewOrder({ ...newOrder, route: e.target.value })} />
                </Grid>
                <Grid size={{ xs: 6, sm: 3 }}>
                  <TextField fullWidth size="small" label={'Частота'}
                    value={newOrder.frequency}
                    onChange={(e) => setNewOrder({ ...newOrder, frequency: e.target.value })} />
                </Grid>
                <Grid size={{ xs: 6, sm: 3 }}>
                  <TextField fullWidth size="small" type="datetime-local" label={'Початок'}
                    value={newOrder.startTime}
                    onChange={(e) => setNewOrder({ ...newOrder, startTime: e.target.value })}
                    slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={{ xs: 6, sm: 3 }}>
                  <TextField fullWidth size="small" type="datetime-local" label={'Кінець'}
                    value={newOrder.endTime}
                    onChange={(e) => setNewOrder({ ...newOrder, endTime: e.target.value })}
                    slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={{ xs: 12, sm: 3 }}>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button variant="contained" size="small" onClick={handleCreate}>
                      {'Створити'}
                    </Button>
                    <Button size="small" onClick={() => setShowForm(false)}>
                      {'Скасувати'}
                    </Button>
                  </Box>
                </Grid>
              </Grid>
            </Paper>
          ) : (
            <Button variant="outlined" size="small" onClick={() => setShowForm(true)}>
              {'+ Нове призначення'}
            </Button>
          )}
        </Box>
      )}

      <TableContainer component={Paper} sx={{ overflowX: 'auto' }}>
        <Table size="small" sx={{ minWidth: 500 }}>
          <TableHead>
            <TableRow>
              <TableCell>{'Препарат'}</TableCell>
              <TableCell>{'Доза'}</TableCell>
              <TableCell>{'Шлях'}</TableCell>
              <TableCell>{'Статус'}</TableCell>
              {showActions && <TableCell>{''}</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {orders.length === 0 ? (
              <TableRow>
                <TableCell colSpan={showActions ? 5 : 4} align="center" sx={{ py: 3, color: theme.palette.text.secondary }}>
                  {'Немає призначень'}
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
                  {showActions && (
                    <TableCell>
                      {order.status === 'ACTIVE' && canExecute && onExecuteOrder && (
                        <IconButton
                          onClick={() => onExecuteOrder(order.id)}
                          sx={{ color: theme.palette.secondary.main, '&:hover': { bgcolor: theme.palette.action.hover } }}
                        >
                          <CheckCircle />
                        </IconButton>
                      )}
                      {order.status === 'ACTIVE' && !canExecute && onCancelOrder && (
                        <Button size="small" color="error" onClick={() => onCancelOrder(order.id)}>
                          {'Скасувати'}
                        </Button>
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
