import { useState, useMemo } from 'react';
import type { PrescriptionItem, PrescriptionDayPart, MedicineCatalogItem, AllergyItem } from '../../types/medication';
import MedicineSearchInput from './MedicineSearchInput';
import PrescriptionSpreadsheet from './PrescriptionSpreadsheet';
import ExecuteDosePopover from './ExecuteDosePopover';
import DeleteConfirmPopover from './DeleteConfirmPopover';

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
  onExecute?: (dayPartId: string, actualDose: string, secondPersonLogin: string, secondPersonPassword: string) => Promise<void>;
  onAddItem: (data: { medicineName: string; medicineMethod?: string; regime?: string }) => Promise<void>;
  onRemoveItem: (itemId: string) => Promise<void>;
  onSearchMedicine: (keyword: string) => Promise<MedicineCatalogItem[]>;
  allergies: AllergyItem[];
  loading?: boolean;
}

export default function PrescriptionGrid({
  items, canEdit, isDoctor, isNurse,
  onPlan, onCancelMedication, onRestoreToPlanned, onCancelAssignment, onAddDay, onRemoveDay, onExecute,
  onAddItem, onRemoveItem,
  onSearchMedicine, allergies, loading,
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

  const openExecute = (dp: PrescriptionDayPart, el: HTMLElement) => {
    if (!canEdit || !isNurse || !dp.isPlanned || dp.isCompleted) return;
    setExecDp(dp);
    setExecDose(dp.dose ?? '');
    setExecuting(false);
    setShow2fa(false);
    setSecondPersonLogin('');
    setSecondPersonPassword('');
    setSecondPersonError('');
    setExecAnchor(el);
  };

  const closeExecute = () => {
    setExecAnchor(null);
    setExecDp(null);
    setShow2fa(false);
    setSecondPersonError('');
  };

  const proceedTo2fa = () => {
    if (!execDose.trim()) return;
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
      await onExecute(execDp.id, execDose, secondPersonLogin, secondPersonPassword);
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

  return (
    <div className="flex flex-col gap-2">
      <MedicineSearchInput
        canEdit={canEdit}
        isDoctor={isDoctor}
        allergies={allergies}
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
      />

      <ExecuteDosePopover
        execAnchor={execAnchor}
        execDp={execDp}
        execDose={execDose}
        onExecDoseChange={setExecDose}
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

      <DeleteConfirmPopover
        deleteAnchor={deleteAnchor}
        deleting={deleting}
        onCloseDeleteConfirm={closeDeleteConfirm}
        onConfirmDelete={confirmDelete}
      />
    </div>
  );
}
