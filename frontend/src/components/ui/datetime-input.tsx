import * as React from "react";

import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { composeActualTime, normalizeTime } from "@/lib/actualTime";

/**
 * Controlled date+time+seconds input built on native `<input type="date">` and
 * `<input type="time" step="1">`. Native pickers give us keyboard navigation,
 * mobile pickers and zero extra dependencies (react-day-picker / Radix calendar
 * were deliberately rejected: they would add a runtime dependency and a Radix
 * stack into a Base-UI project, conflicting with the repo's styling primitives
 * and the "no unjustified dependencies" policy).
 */
export interface DatetimeInputProps {
  value: string | null;
  onChange: (value: string | null) => void;
  disabled?: boolean;
  required?: boolean;
  invalid?: boolean;
  min?: string;
  max?: string;
  dateAriaLabel: string;
  timeAriaLabel: string;
  id?: string;
  className?: string;
  "aria-describedby"?: string;
}

function splitValue(value: string | null): { date: string; time: string } {
  if (!value) return { date: "", time: "" };
  const [d, t] = value.split("T");
  return { date: d || "", time: (t || "").slice(0, 8) };
}

export function DatetimeInput({
  value,
  onChange,
  disabled,
  required,
  invalid,
  min,
  max,
  dateAriaLabel,
  timeAriaLabel,
  id,
  className,
  "aria-describedby": describedBy,
}: DatetimeInputProps) {
  const [parts, setParts] = React.useState(() => splitValue(value));

  React.useEffect(() => {
    setParts(splitValue(value));
  }, [value]);

  const minDate = min?.slice(0, 10);
  const minTime = min?.slice(11, 19);
  const maxDate = max?.slice(0, 10);
  const maxTime = max?.slice(11, 19);

  const handleDate = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newDate = e.target.value;
    const next = { ...parts, date: newDate };
    setParts(next);
    if (!newDate || !next.time) {
      onChange(null);
      return;
    }
    const composed = composeActualTime(newDate, normalizeTime(next.time));
    onChange(composed);
  };

  const handleTime = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newTime = e.target.value;
    const next = { ...parts, time: newTime };
    setParts(next);
    if (!newTime || !next.date) {
      onChange(null);
      return;
    }
    const composed = composeActualTime(next.date, normalizeTime(newTime));
    onChange(composed);
  };

  const inputClass = "appearance-none [&::-webkit-calendar-picker-indicator]:hidden [&::-webkit-calendar-picker-indicator]:appearance-none";

  return (
    <div className={cn("flex gap-2", className)}>
      <Input
        id={id}
        type="date"
        value={parts.date}
        onChange={handleDate}
        disabled={disabled}
        required={required}
        min={minDate}
        max={maxDate}
        aria-label={dateAriaLabel}
        aria-invalid={invalid || undefined}
        aria-describedby={describedBy}
        className={inputClass}
      />
      <Input
        type="time"
        step={1}
        value={parts.time}
        onChange={handleTime}
        disabled={disabled}
        required={required}
        min={minTime}
        max={maxTime}
        aria-label={timeAriaLabel}
        aria-invalid={invalid || undefined}
        aria-describedby={describedBy}
        className={inputClass}
      />
    </div>
  );
}
