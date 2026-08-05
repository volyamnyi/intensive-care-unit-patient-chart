import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

export function SetupSteps({ current, className }: { current: 1 | 2 | 3 | 4; className?: string }) {
  const steps = ["Пацієнт", "Замовлення", "Перегляд", "Маршрут"];
  return (
    <ol className={cn("flex flex-wrap items-center gap-3", className)}>
      {steps.map((s, i) => {
        const n = i + 1;
        const done = n < current;
        const active = n === current;
        return (
          <li key={s} className="flex items-center gap-3">
            <span
              className={cn(
                "flex items-center gap-2 rounded-full border px-3 py-1.5 text-sm",
                active && "border-accent bg-accent text-accent-foreground",
                done && "border-mint bg-mint text-mint-foreground",
                !active && !done && "border-border text-muted-foreground",
              )}
            >
              <span className="grid size-5 place-items-center rounded-full bg-background/40 text-xs font-semibold">
                {done ? <Check className="size-3" /> : n}
              </span>
              {s}
            </span>
            {n < steps.length && <span className="h-px w-6 bg-border" />}
          </li>
        );
      })}
    </ol>
  );
}