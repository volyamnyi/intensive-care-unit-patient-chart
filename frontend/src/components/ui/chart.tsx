import * as React from 'react';
import { ResponsiveContainer, Tooltip } from 'recharts';
import { cn } from '@/lib/utils';

/**
 * shadcn-style chart primitives ported for Base UI + Tailwind v4
 * (manufacturing epic #271, issue #279). Trimmed to what the production
 * trend chart needs: container with config-driven CSS vars, tooltip and
 * tooltip content. Colors come from `--chart-*` tokens in `index.css`.
 */

export type ChartConfig = Record<
  string,
  {
    label?: React.ReactNode;
    color?: string;
    theme?: {
      light: string;
      dark: string;
    };
  }
>;

function ChartStyle({ id, config }: { id: string; config: ChartConfig }) {
  const colorConfig = Object.entries(config).filter(
    ([, itemConfig]) => itemConfig.theme || itemConfig.color,
  );
  if (colorConfig.length === 0) return null;
  const css = [
    `[data-chart="${id}"] {`,
    ...colorConfig.map(([key, itemConfig]) => `  --color-${key}: ${itemConfig.color};`),
    '}',
    `.dark [data-chart="${id}"] {`,
    ...colorConfig.map(([key, itemConfig]) =>
      itemConfig.theme?.dark
        ? `  --color-${key}: ${itemConfig.theme.dark};`
        : `  --color-${key}: ${itemConfig.color};`,
    ),
    '}',
  ].join('\n');
  return <style dangerouslySetInnerHTML={{ __html: css }} />;
}

function ChartContainer({
  id,
  className,
  children,
  config,
  ...props
}: React.ComponentProps<'div'> & {
  config: ChartConfig;
  children: React.ComponentProps<typeof ResponsiveContainer>['children'];
}) {
  const uniqueId = React.useId();
  const chartId = `chart-${id ?? uniqueId.replace(/:/g, '')}`;
  return (
    <div
      data-slot="chart"
      data-chart={chartId}
      className={cn(
        'flex aspect-auto justify-center overflow-hidden text-xs [&_.recharts-cartesian-axis-tick_text]:fill-muted-foreground',
        className,
      )}
      {...props}
    >
      <ChartStyle id={chartId} config={config} />
      <ResponsiveContainer width="100%" height="100%">
        {children}
      </ResponsiveContainer>
    </div>
  );
}

export interface ChartTooltipRow {
  dataKey?: string | number;
  name?: string | number;
  value?: number | string | Array<number | string>;
  color?: string;
  payload?: Record<string, unknown>;
}

function ChartTooltipContent({
  active,
  label,
  payload,
  className,
  labelFormatter,
  hideLabel = false,
}: {
  active?: boolean;
  label?: string | number;
  payload?: ChartTooltipRow[];
  className?: string;
  labelFormatter?: (label: string, payload: ChartTooltipRow[]) => React.ReactNode;
  hideLabel?: boolean;
}) {
  if (!active || !payload || payload.length === 0) return null;
  return (
    <div
      data-slot="chart-tooltip-content"
      className={cn(
        'rounded-lg border bg-popover px-2.5 py-1.5 text-xs text-popover-foreground shadow-md outline-none',
        className,
      )}
    >
      {!hideLabel && label != null && (
        <div className="mb-1 font-medium">
          {labelFormatter ? labelFormatter(String(label), payload) : label}
        </div>
      )}
      <div className="flex flex-col gap-1">
        {payload.map((row, index) => (
          <div key={`${row.dataKey ?? index}`} className="flex items-center gap-1.5">
            <span
              className="size-2.5 shrink-0 rounded-[2px]"
              style={{ backgroundColor: row.color ?? 'var(--chart-1)' }}
            />
            <span className="text-muted-foreground">{row.name}</span>
            <span className="ml-auto pl-2 font-medium tabular-nums">
              {Array.isArray(row.value) ? row.value.join(', ') : row.value}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

const ChartTooltip = Tooltip;

export { ChartContainer, ChartTooltip, ChartTooltipContent, ChartStyle };
