import { useState, useMemo } from 'react';
import type { PrescriptionItem, PrescriptionDayPart, MedicineCatalogItem, PrescriptionInteractionsResponse } from '../../types/medication';
import MedicineSearchInput from './MedicineSearchInput';
import PrescriptionSpreadsheet from './PrescriptionSpreadsheet';
import ExecuteDosePopover from './ExecuteDosePopover';
import DeleteConfirmPopover from './DeleteConfirmPopover';
import ActualTimeCorrectionDialog from './ActualTimeCorrectionDialog';
import { actualTimeErrorMessage, currentLocalIso, validateActualTime } from '@/lib/actualTime';

export interface GridItem extends PrescriptionItem {
  cells: Map<string, PrescriptionDayPart>;
}

export interface GridProps {
  items: PrescriptionItem[];
  canEdit: boolean;
  isDoctor: boolean;
  isNurse: boolean;
  onPlan: (dayPartId: string, dose: string) => Promise<void>;
  onCancelMedication: (dayPartId: string) => Promise<void>;
  onRestoreToPlanned: (dayPartId: string) => Promise<void>;
  onCancelAssignment: (dayPartId: string) => Promise<void>;
  onAddDay?: (itemId: string) => Promise<void> | void;
  onRemoveDay?: (itemId: string, dayId: string) => Promise<void> | void;
  onExecute?: (
    dayPartId: string,
    actualDose: string,
    secondPersonLogin: string,
    secondPersonPassword: string,
    administeredAt?: string,
    dispensedAt?: string,
  ) => Promise<void>;
  onAddItem: (data: { medicineName: string; medicineMethod?: string; regime?: string; medicineAtcCode?: string | null }) => Promise<void>;
  onRemoveItem: (itemId: string) => Promise<void>;
  onSearchMedicine: (keyword: string, signal?: AbortSignal) => Promise<MedicineCatalogItem[]>;
  loading?: boolean;
  /** Server-computed interaction warnings (#304); null while loading / not fetched. */
  interactions?: PrescriptionInteractionsResponse | null;
  /** True when the user holds PRESCRIPTION_ACTUAL_TIME_EDIT (#444/#446). */
  canCorrectActualTime?: boolean;
  /** Called after a successful actual-time correction so the grid can refresh. */
  onActualTimeUpdated?: () => void | Promise<void>;
}

export default function PrescriptionGrid({
  items, canEdit, isDoctor, isNurse,
  onPlan, onCancelMedication, onRestoreToPlanned, onCancelAssignment, onAddDay, onRemoveDay, onExecute,
  onAddItem, onRemoveItem,
  onSearchMedicine, loading, interactions,
  canCorrectActualTime, onActualTimeUpdated,
}: GridProps) {

  const allDates = useMemo(() => {
    const set = new Set<string>();
    items.forEach(it => it.dayParts?.forEach(dp => {
      if (dp.dayDate) set.add(dp.dayDate);
    }));
    return Array.from(set).sort();
  }, [items]);

  const today = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const startIdx = Math.max(0, allDates.indexOf(
    allDates.find(d => d >= today) ?? allDates[0] ?? ''
  ));
  const [viewStart, setViewStart] = useState(startIdx);
  const daysToShow = 7;
  const visibleDates = allDates.slice(viewStart, viewStart + daysToShow);

  const shiftLeft = () => setViewStart(Math.max(0, viewStart - daysToShow));
  const shiftRight = () => {
    if (viewStart + daysToShow < allDates.length) setViewStart(viewStart + daysToShow);
  };

  const gridItems: GridItem[] = useMemo(() =>
    items.map(item => {
      const cells = new Map<string, PrescriptionDayPart>();
      item.dayParts?.forEach(dp => {
        if (dp.dayDate) cells.set(`${dp.dayDate}|${dp.period}`, dp);
      });
      return { ...item, cells };
    }), [items]);

  const [execAnchor, setExecAnchor] = useState<HTMLElement | null>(null);
  const [execDp, setExecDp] = useState<PrescriptionDayPart | null>(null);
  const [execDose, setExecDose] = useState('');
  const [executing, setExecuting] = useState(false);
  const [show2fa, setShow2fa] = useState(false);
  const [secondPersonLogin, setSecondPersonLogin] = useState('');
  const [secondPersonPassword, setSecondPersonPassword] = useState('');
  const [secondPersonError, setSecondPersonError] = useState('');
  // Actual administration/dispensing time (#443/#446): a medical fact captured
  // at execution time, editable only through the dedicated correction dialog.
  const [execAdministeredAt, setExecAdministeredAt] = useState<string | null>(null);
  const [execDispensedEnabled, setExecDispensedEnabled] = useState(false);
  const [execDispensedAt, setExecDispensedAt] = useState<string | null>(null);
  const [execMinDate, setExecMinDate] = useState<string | undefined>(undefined);
  const [execMaxDate, setExecMaxDate] = useState<string | undefined>(undefined);

  const openExecute = (dp: PrescriptionDayPart, el: HTMLElement, minDate?: string) => {
    if (!canEdit || !isNurse || !dp.isPlanned || dp.isCompleted) return;
    setExecDp(dp);
    setExecDose(dp.dose ?? '');
    setExecuting(false);
    setShow2fa(false);
    setSecondPersonLogin('');
    setSecondPersonPassword('');
    setSecondPersonError('');
    setExecAdministeredAt(currentLocalIso());
    setExecDispensedEnabled(false);
    setExecDispensedAt(null);
    setExecMinDate(minDate);
    setExecMaxDate(dp.dayDate);
    setExecAnchor(el);
  };

  const closeExecute = () => {
    setExecAnchor(null);
    setExecDp(null);
    setShow2fa(false);
    setSecondPersonError('');
  };

  // Inline mirror of the server-side V1-V4 matrix (#443). The server stays the
  // source of truth - this only blocks obviously invalid values before a
  // request is sent.
  const execTimeError = useMemo(() => {
    if (!execAdministeredAt) return 'Вкажіть фактичний час введення';
    const code = validateActualTime(execAdministeredAt, {
      minDate: execMinDate,
      maxDate: execMaxDate,
      nowIso: currentLocalIso(),
      dispensedIso: execDispensedEnabled ? (execDispensedAt ?? undefined) : undefined,
    });
    if (code !== 'OK') return actualTimeErrorMessage(code);
    if (execDispensedEnabled && !execDispensedAt) return 'Вкажіть фактичний час видачі';
    return null;
  }, [execAdministeredAt, execDispensedEnabled, execDispensedAt, execMinDate, execMaxDate]);

  const proceedTo2fa = () => {
    if (!execDose.trim() || execTimeError) return;
    setSecondPersonLogin('');
    setSecondPersonPassword('');
    setSecondPersonError('');
    setShow2fa(true);
  };

  const commitExecute = async () => {
    if (!execDp || !onExecute) return;
    setExecuting(true);
    setSecondPersonError('');
    try {
      await onExecute(
        execDp.id, execDose, secondPersonLogin, secondPersonPassword,
        execAdministeredAt ?? undefined,
        execDispensedEnabled ? (execDispensedAt ?? undefined) : undefined,
      );
      closeExecute();
    } catch (err: any) {
      setSecondPersonError(err?.response?.data?.message || err?.message || 'Помилка 2FA');
    } finally {
      setExecuting(false);
    }
  };

  const [deleteAnchor, setDeleteAnchor] = useState<HTMLElement | null>(null);
  const [deleteItemId, setDeleteItemId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const openDeleteConfirm = (itemId: string, el: HTMLElement) => {
    setDeleteItemId(itemId);
    setDeleteAnchor(el);
  };

  const closeDeleteConfirm = () => { setDeleteAnchor(null); setDeleteItemId(null); };

  const confirmDelete = async () => {
    if (!deleteItemId) return;
    setDeleting(true);
    try {
      await onRemoveItem(deleteItemId);
      closeDeleteConfirm();
    } finally {
      setDeleting(false);
    }
  };

  // Correction of an already recorded actual time (#444/#446) - gated by
  // PRESCRIPTION_ACTUAL_TIME_EDIT; for everyone else the completed cell stays
  // a no-op, exactly as before.
  const [correctionDp, setCorrectionDp] = useState<PrescriptionDayPart | null>(null);
  const [correctionMinDate, setCorrectionMinDate] = useState<string | undefined>(undefined);

  const openActualTimeCorrection = (dp: PrescriptionDayPart, minDate?: string) => {
    if (!canCorrectActualTime) return;
    setCorrectionDp(dp);
    setCorrectionMinDate(minDate);
  };

  const closeActualTimeCorrection = () => {
    setCorrectionDp(null);
    setCorrectionMinDate(undefined);
  };

  return (
    <div className="flex flex-col gap-2">
      <MedicineSearchInput
        canEdit={canEdit}
        isDoctor={isDoctor}
        onAddItem={onAddItem}
        onSearchMedicine={onSearchMedicine}
      />

      <PrescriptionSpreadsheet
        canEdit={canEdit}
        isDoctor={isDoctor}
        isNurse={isNurse}
        gridItems={gridItems}
        visibleDates={visibleDates}
        allDates={allDates}
        viewStart={viewStart}
        daysToShow={daysToShow}
        loading={loading}
        onShiftLeft={shiftLeft}
        onShiftRight={shiftRight}
        onAddDay={onAddDay}
        onRemoveDay={onRemoveDay}
        onPlan={onPlan}
        onCancelMedication={onCancelMedication}
        onRestoreToPlanned={onRestoreToPlanned}
        onCancelAssignment={onCancelAssignment}
        onOpenExecute={openExecute}
        onOpenDeleteConfirm={openDeleteConfirm}
        interactions={interactions}
        canCorrectActualTime={canCorrectActualTime}
        onOpenActualTimeCorrection={openActualTimeCorrection}
      />

      <ExecuteDosePopover
        execAnchor={execAnchor}
        execDp={execDp}
        execDose={execDose}
        onExecDoseChange={setExecDose}
        execAdministeredAt={execAdministeredAt}
        onExecAdministeredAtChange={setExecAdministeredAt}
        execDispensedEnabled={execDispensedEnabled}
        onExecDispensedEnabledChange={setExecDispensedEnabled}
        execDispensedAt={execDispensedAt}
        onExecDispensedAtChange={setExecDispensedAt}
        execTimeError={execTimeError}
        onExecTimeNow={() => setExecAdministeredAt(currentLocalIso())}
        execMinDate={execMinDate}
        execMaxDate={execMaxDate}
        executing={executing}
        show2fa={show2fa}
        secondPersonLogin={secondPersonLogin}
        onSecondPersonLoginChange={setSecondPersonLogin}
        secondPersonPassword={secondPersonPassword}
        onSecondPersonPasswordChange={setSecondPersonPassword}
        secondPersonError={secondPersonError}
        onCloseExecute={closeExecute}
        onProceedTo2fa={proceedTo2fa}
        onCommitExecute={commitExecute}
        onShow2faChange={setShow2fa}
      />

      <ActualTimeCorrectionDialog
        open={Boolean(correctionDp)}
        onOpenChange={(open) => { if (!open) closeActualTimeCorrection(); }}
        dayPart={correctionDp}
        minDate={correctionMinDate}
        onUpdated={onActualTimeUpdated}
      />

      <DeleteConfirmPopover
        deleteAnchor={deleteAnchor}
        deleting={deleting}
        onCloseDeleteConfirm={closeDeleteConfirm}
        onConfirmDelete={confirmDelete}
      />
    </div>
  );
}
