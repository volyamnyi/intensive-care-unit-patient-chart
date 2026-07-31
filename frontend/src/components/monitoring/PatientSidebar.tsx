import { FileText, User, Weight, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
import {
  Sidebar, SidebarRail, SidebarHeader, SidebarContent, SidebarGroup,
} from '../ui/Sidebar';
import ScaleResultsPanel from '../common/ScaleResultsPanel';
import LabResultsPanel from '../common/LabResultsPanel';
import VentilationPanel from '../common/VentilationPanel';
import PatientStatePanel from '../common/PatientStatePanel';
import type { Episode, ClinicalDay, FluidBalanceItem, LabResult, VentilationSettings, PatientStateAssessment, ClinicalScale } from '../../types';
import type { LabResultCreateRequest, VentilationCreateRequest, PatientStateCreateRequest } from '../../types';

interface NoteItem {
  id: string;
  text: string;
  authorId?: string | null;
  role?: string | null;
  createdAt?: string | null;
}

interface ScaleItem {
  id: string;
  name?: string;
  result: string;
}

export interface PatientSidebarProps {
  episode: Episode;
  selectedDay: ClinicalDay | null;
  isLocked: boolean;
  notes: NoteItem[];
  noteText: string;
  autoSaveStatus: string;
  savingNote: boolean;
  scales: ScaleItem[];
  ventilation: unknown[];
  labs: unknown[];
  patientState: unknown[];
  loadingSidebar: boolean;
  balanceItems: FluidBalanceItem[];
  totalIntake: number;
  totalOutput: number;
  dailyBalance: number;
  cumulativeBalance: number;
  keyScales: { name: string; result: string }[];
  canEditSidebar: boolean;
  onNoteChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onSaveNote: () => void;
  onCreateLab: (data: LabResultCreateRequest) => Promise<void>;
  onCreateVentilation: (data: VentilationCreateRequest) => Promise<void>;
  onCreatePatientState: (data: PatientStateCreateRequest) => Promise<void>;
  availableScales?: ClinicalScale[];
  onCreateScale?: (scaleId: string, result: string) => void;
  onCalculateScale?: (scaleId: string, rawData: Record<string, unknown>) => void;
  episodeId?: string;
}

function EmptyState({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <div className="flex flex-col items-center py-1.5 opacity-60">
      {icon}
      <p className="text-xs text-muted-foreground mt-0.5">{text}</p>
    </div>
  );
}

export default function PatientSidebar({
  episode, selectedDay, isLocked,
  notes, noteText, autoSaveStatus, savingNote,
  scales, ventilation, labs, patientState, loadingSidebar,
  balanceItems, totalIntake, totalOutput, dailyBalance, cumulativeBalance,
  keyScales, canEditSidebar,
  onNoteChange, onSaveNote,
  onCreateLab, onCreateVentilation, onCreatePatientState,
  availableScales, onCreateScale, onCalculateScale, episodeId,
}: PatientSidebarProps) {
  return (
    <Sidebar side="right" collapsible="none">
      <SidebarRail />
      <SidebarHeader>
        <p className="font-bold text-xs mb-0.75 flex items-center gap-0.5">
          <User className="size-4" /> Пацієнт
        </p>
        <p className="text-sm font-semibold">{episode.patientName || '\u2014'}</p>
        <p className="text-xs text-muted-foreground mt-0.5">
          Діагноз: {episode.admissionDiagnosis || '\u2014'}
        </p>
        <p className="text-xs text-muted-foreground">
          {[episode.ward, episode.bedNumber].filter(Boolean).join(' / ') || '\u2014'}
          {episode.heightCm ? ` \u00B7 ${episode.heightCm} см` : ''}
        </p>
        {selectedDay?.weightKg && (
          <p className="text-xs text-muted-foreground">Вага: {selectedDay.weightKg} кг</p>
        )}
        {keyScales.length > 0 && (
          <div className="mt-0.75 flex flex-wrap gap-0.5">
            {keyScales.map(s => (
              <span key={s.name} className="text-xs rounded px-0.75 py-0.25 bg-muted">
                <b>{s.name}</b>: {s.result}
              </span>
            ))}
          </div>
        )}
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup label="Баланс рідини">
          <div className="flex justify-between text-xs py-0.25">
            <span>{'Надійшло:'}</span><b>{totalIntake} {'мл'}</b>
          </div>
          <div className="flex justify-between text-xs py-0.25">
            <span>{'Виділено:'}</span><b>{totalOutput} {'мл'}</b>
          </div>
          <div className="flex justify-between text-xs py-0.25 border-t border-border pt-0.5 mt-0.25">
            <span>{'Добовий баланс:'}</span>
            <b className={cn(dailyBalance < 0 ? 'text-destructive' : 'text-success')}>{dailyBalance >= 0 ? '+' : ''}{dailyBalance}</b>
          </div>
          <div className="flex justify-between text-xs py-0.25">
            <span>{'Кумулятивний баланс:'}</span>
            <b className={cn(cumulativeBalance < 0 ? 'text-destructive' : 'text-success')}>{cumulativeBalance >= 0 ? '+' : ''}{cumulativeBalance}</b>
          </div>
        </SidebarGroup>

        <SidebarGroup label="Нотатки" count={notes.length}>
          {notes.length === 0 ? (
            <EmptyState icon={<FileText className="size-6" />} text={'Немає нотаток. Додайте нову нотатку вище.'} />
          ) : (
            <div className="space-y-1 py-0">
              {notes.map((n) => (
                <div key={n.id} className="px-0 flex flex-col items-start">
                  <p className="text-xs">{n.text}</p>
                  <p className="text-[10px] text-muted-foreground">
                    {[n.role, n.createdAt ? new Date(n.createdAt).toLocaleString('uk-UA') : null]
                      .filter(Boolean)
                      .join(' \u00B7 ')}
                  </p>
                </div>
              ))}
            </div>
          )}
          {canEditSidebar && (
            <div className="flex flex-col gap-0.5 mt-0.5">
              <Input
                placeholder={'Нова нотатка'}
                value={noteText}
                onChange={onNoteChange}
                aria-label="Нова нотатка"
              />
              <div className="flex gap-1 items-center">
                <Button size="sm" variant="outline" onClick={onSaveNote} disabled={savingNote || !noteText.trim()}>
                  {'Додати нотатку'}
                </Button>
                {autoSaveStatus === 'saving' && (
                  <div className="flex items-center gap-0.5">
                    <Loader2 className="size-[10px] animate-spin" />
                    <span className="text-[10px] text-muted-foreground">{'Зберігається...'}</span>
                  </div>
                )}
                {autoSaveStatus === 'saved' && (
                  <span className="text-[10px] text-success">{'Збережено'}</span>
                )}
                {autoSaveStatus === 'error' && (
                  <span className="text-[10px] text-destructive">{'Помилка'}</span>
                )}
              </div>
            </div>
          )}
        </SidebarGroup>

        <SidebarGroup label="Шкали" count={scales.length}>
          {scales.length === 0 && !availableScales?.length ? (
            <EmptyState icon={<Weight className="size-6" />} text={'Немає даних шкал'} />
          ) : (
            <ScaleResultsPanel
              results={scales}
              availableScales={availableScales ?? []}
              onCreateResult={onCreateScale}
              onCalculateScale={onCalculateScale}
              disabled={!canEditSidebar}
              episodeId={episodeId}
            />
          )}
        </SidebarGroup>

        <SidebarGroup label="ШВЛ" count={ventilation.length}>
          <VentilationPanel
            clinicalDayId={selectedDay?.id ?? ''}
            ventilation={ventilation as VentilationSettings[]}
            isLocked={isLocked}
            onCreate={onCreateVentilation}
          />
        </SidebarGroup>

        <SidebarGroup label="Лабораторні результати" count={labs.length}>
          <LabResultsPanel
            clinicalDayId={selectedDay?.id ?? ''}
            labs={labs as LabResult[]}
            isLocked={isLocked}
            onCreate={onCreateLab}
          />
        </SidebarGroup>

        <SidebarGroup label="Стан пацієнта" count={patientState.length}>
          <PatientStatePanel
            clinicalDayId={selectedDay?.id ?? ''}
            assessments={patientState as PatientStateAssessment[]}
            isLocked={isLocked}
            onCreate={onCreatePatientState}
          />
        </SidebarGroup>

        {loadingSidebar && (
          <div className="flex justify-center"><Loader2 className="size-4 animate-spin" /></div>
        )}
      </SidebarContent>
    </Sidebar>
  );
}
