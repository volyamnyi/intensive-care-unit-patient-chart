import { cn } from '@/lib/utils';

interface DocumentHeaderProps {
  institutionName: string;
  institutionEdrpou: string;
  patientName: string | null;
  patientAge: string | null;
  dayNumber: number | null;
  dayDate: string | null;
  cardNumber: string | null;
}

function formatDate(iso: string | null): string {
  if (!iso) return '\u2014';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '\u2014';
  return d.toLocaleDateString('uk-UA', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export default function DocumentHeader({
  institutionName,
  institutionEdrpou,
  patientName,
  patientAge,
  dayNumber,
  dayDate,
  cardNumber,
}: DocumentHeaderProps) {
  return (
    <div className="document-header mb-2 rounded-xl border bg-card p-3 text-card-foreground shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-wide">
            Міністерство охорони здоров'я України
          </p>
          <p className="text-sm font-semibold">{institutionName || '\u2014'}</p>
          {institutionEdrpou && (
            <p className="text-[11px] text-muted-foreground">
              {`Код за ЄДРПОУ: ${institutionEdrpou}`}
            </p>
          )}
        </div>
        <div className="text-right">
          <p className="text-[11px] font-semibold uppercase tracking-wide">Медична документація</p>
          <p className="text-[11px]">
            Форма первинної облікової документації № 003-15/о
          </p>
          <p className="text-[10px] text-muted-foreground">
            {`Затверджено наказом МОЗ України від 03.11.2025 № 1675`}
          </p>
        </div>
      </div>

      <p className="my-1.5 text-center font-rubik text-base font-extrabold uppercase tracking-wide">
        Карта інтенсивної терапії
      </p>

      <div className="flex flex-wrap items-center gap-x-3 gap-y-0.5 text-[11px]">
        <span>
          <b>№ карти:</b> {cardNumber || '\u2014'}
        </span>
        <span>
          <b>Дата заповнення:</b> {formatDate(dayDate)}
        </span>
        <span>
          <b>Доба перебування у ВАІТ:</b> {dayNumber ?? '\u2014'}
        </span>
        {patientAge && (
          <span>
            <b>Вік пацієнта:</b> {patientAge}
          </span>
        )}
        <span className={cn('ml-auto max-w-[320px] truncate')}>
          <b>Пацієнт:</b> {patientName || '\u2014'}
        </span>
      </div>
    </div>
  );
}
