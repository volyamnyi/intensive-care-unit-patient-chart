import { useState } from 'react';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation();
  const statusLabels: Record<string, string> = {
    DRAFT: t('medicalOrders.statusDraft'),
    ACTIVE: t('medicalOrders.statusActive'),
    COMPLETED: t('medicalOrders.statusCompleted'),
    CANCELLED: t('medicalOrders.statusCancelled'),
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
                {t('medicalOrders.formTitle')}
              </Typography>
              <Grid container spacing={1} sx={{ alignItems: 'center' }}>
                <Grid size={3}>
                  <TextField fullWidth size="small" label={t('medicalOrders.category')}
                    value={newOrder.category}
                    onChange={(e) => setNewOrder({ ...newOrder, category: e.target.value })} />
                </Grid>
                <Grid size={3}>
                  <TextField fullWidth size="small" label={t('medicalOrders.drugName')}
                    value={newOrder.drugName}
                    onChange={(e) => setNewOrder({ ...newOrder, drugName: e.target.value })} />
                </Grid>
                <Grid size={2}>
                  <TextField fullWidth size="small" label={t('medicalOrders.dose')}
                    value={newOrder.dose}
                    onChange={(e) => setNewOrder({ ...newOrder, dose: e.target.value })} />
                </Grid>
                <Grid size={2}>
                  <TextField fullWidth size="small" label={t('medicalOrders.unit')}
                    value={newOrder.unit}
                    onChange={(e) => setNewOrder({ ...newOrder, unit: e.target.value })} />
                </Grid>
                <Grid size={2}>
                  <TextField fullWidth size="small" label={t('medicalOrders.route')}
                    value={newOrder.route}
                    onChange={(e) => setNewOrder({ ...newOrder, route: e.target.value })} />
                </Grid>
                <Grid size={3}>
                  <TextField fullWidth size="small" label={t('medicalOrders.frequency')}
                    value={newOrder.frequency}
                    onChange={(e) => setNewOrder({ ...newOrder, frequency: e.target.value })} />
                </Grid>
                <Grid size={3}>
                  <TextField fullWidth size="small" type="datetime-local" label={t('medicalOrders.startTime')}
                    value={newOrder.startTime}
                    onChange={(e) => setNewOrder({ ...newOrder, startTime: e.target.value })}
                    slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={3}>
                  <TextField fullWidth size="small" type="datetime-local" label={t('medicalOrders.endTime')}
                    value={newOrder.endTime}
                    onChange={(e) => setNewOrder({ ...newOrder, endTime: e.target.value })}
                    slotProps={{ inputLabel: { shrink: true } }} />
                </Grid>
                <Grid size={3}>
                  <Button variant="contained" size="small" onClick={handleCreate}>
                    {t('medicalOrders.createButton')}
                  </Button>
                  <Button size="small" sx={{ ml: 1 }} onClick={() => setShowForm(false)}>
                    {t('medicalOrders.cancelButton')}
                  </Button>
                </Grid>
              </Grid>
            </Paper>
          ) : (
            <Button variant="outlined" size="small" onClick={() => setShowForm(true)}>
              {t('medicalOrders.newOrderButton')}
            </Button>
          )}
        </Box>
      )}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{t('medicalOrders.tableHeaders.drugName')}</TableCell>
              <TableCell>{t('medicalOrders.tableHeaders.dose')}</TableCell>
              <TableCell>{t('medicalOrders.tableHeaders.route')}</TableCell>
              <TableCell>{t('medicalOrders.tableHeaders.status')}</TableCell>
              {showActions && <TableCell>{t('medicalOrders.tableHeaders.actions')}</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {orders.length === 0 ? (
              <TableRow>
                <TableCell colSpan={showActions ? 5 : 4} align="center" sx={{ py: 3, color: theme.palette.text.secondary }}>
                  {t('medicalOrders.empty')}
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
                          {t('medicalOrders.cancelOrderButton')}
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
