import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Area, AreaChart, CartesianGrid, XAxis, YAxis } from 'recharts';
import { Button } from '@/components/ui/button';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart';
import { productionApi } from '@/api/prosthetics';
import { useAuth } from '@/services/AuthContext';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { getErrorMessage } from '@/utils/errorMessage';
import { bucketTrend } from '@/lib/productionTrend';
import type { ProductionWorkItem } from '@/prosthetics/types';

const RANGES = [7, 30, 90] as const;

const chartConfig = {
  created: { label: 'Створено', color: 'var(--chart-1)' },
  completed: { label: 'Завершено', color: 'var(--chart-2)' },
  failed: { label: 'Провалено', color: 'var(--chart-3)' },
} satisfies ChartConfig;

function Legend({ showFailed }: { showFailed: boolean }) {
  const entries = [
    { key: 'created', label: 'Створено', color: 'var(--chart-1)' },
    { key: 'completed', label: 'Завершено', color: 'var(--chart-2)' },
    ...(showFailed ? [{ key: 'failed', label: 'Провалено', color: 'var(--chart-3)' }] : []),
  ];
  return (
    <div className="flex flex-wrap gap-3" aria-label="Легенда графіка">
      {entries.map((entry) => (
        <span key={entry.key} className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <span
            className="size-2.5 shrink-0 rounded-[2px]"
            style={{ backgroundColor: entry.color }}
          />
          {entry.label}
        </span>
      ))}
    </div>
  );
}

/**
 * Throughput area chart (epic #271, issue #279): created / completed /
 * failed items per day over 7/30/90 days. Client-side buckets over the
 * permission-scoped list endpoint (dashboard-scale volumes; a dedicated
 * aggregation endpoint is a future perf follow-up, not v1).
 * Rendered by the dashboard page for VIEW_ALL holders only; the failed
 * series additionally requires QUALITY_VIEW.
 */
export default function ProductionTrendChart() {
  const { hasPermission } = useAuth();
  const showFailed = hasPermission('PROSTHETICS_PRODUCTION_QUALITY_VIEW');
  const reduceMotion = useMediaQuery('(prefers-reduced-motion: reduce)');
  const [range, setRange] = useState<(typeof RANGES)[number]>(30);
  const [items, setItems] = useState<ProductionWorkItem[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const fetchItems = useCallback(async () => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    setLoading(true);
    setError(null);
    try {
      const res = await productionApi.list({ page: 0, size: 1000 }, controller.signal);
      setItems(res.data.content);
    } catch (err) {
      if ((err as { code?: string })?.code === 'ERR_CANCELED') return;
      setError(getErrorMessage(err, 'Не вдалося завантажити аналітику'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchItems();
    return () => abortRef.current?.abort();
  }, [fetchItems]);

  const points = useMemo(
    () => (items === null ? [] : bucketTrend(items, range)),
    [items, range],
  );
  const isEmpty = points.length > 0 && points.every((p) => p.created + p.completed + p.failed === 0);

  return (
    <Card>
      <CardHeader className="flex flex-wrap items-center justify-between gap-2">
        <CardTitle className="text-base">Динаміка виробництва</CardTitle>
        <Tabs value={String(range)} onValueChange={(v) => setRange(Number(v) as (typeof RANGES)[number])}>
          <TabsList aria-label="Період">
            {RANGES.map((days) => (
              <TabsTrigger key={days} value={String(days)}>
                {days}д
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>
      </CardHeader>
      <CardContent className="space-y-3">
        <Legend showFailed={showFailed} />
        {loading ? (
          <Skeleton className="h-[260px] w-full" />
        ) : error ? (
          <Alert variant="destructive">
            <AlertTitle>Помилка</AlertTitle>
            <AlertDescription className="flex flex-wrap items-center gap-2">
              {error}
              <Button variant="outline" size="sm" onClick={() => void fetchItems()}>
                Спробувати знову
              </Button>
            </AlertDescription>
          </Alert>
        ) : isEmpty ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Немає даних за період
          </p>
        ) : (
          <ChartContainer
            config={chartConfig}
            className="h-[260px] w-full"
          >
            <AreaChart
              accessibilityLayer
              data={points}
              margin={{ left: -16, right: 8, top: 8 }}
            >
              <defs>
                <linearGradient id="trendCreated" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="var(--color-created)" stopOpacity={0.8} />
                  <stop offset="95%" stopColor="var(--color-created)" stopOpacity={0.1} />
                </linearGradient>
                <linearGradient id="trendCompleted" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="var(--color-completed)" stopOpacity={0.8} />
                  <stop offset="95%" stopColor="var(--color-completed)" stopOpacity={0.1} />
                </linearGradient>
                {showFailed && (
                  <linearGradient id="trendFailed" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--color-failed)" stopOpacity={0.8} />
                    <stop offset="95%" stopColor="var(--color-failed)" stopOpacity={0.1} />
                  </linearGradient>
                )}
              </defs>
              <CartesianGrid vertical={false} />
              <XAxis
                dataKey="label"
                tickLine={false}
                axisLine={false}
                tickMargin={8}
                minTickGap={24}
              />
              <YAxis tickLine={false} axisLine={false} allowDecimals={false} width="auto" />
              <ChartTooltip content={<ChartTooltipContent />} />
              <Area
                dataKey="created"
                name="Створено"
                type="monotone"
                stackId="trend"
                fill="url(#trendCreated)"
                stroke="var(--color-created)"
                strokeWidth={2}
                isAnimationActive={!reduceMotion}
              />
              <Area
                dataKey="completed"
                name="Завершено"
                type="monotone"
                stackId="trend"
                fill="url(#trendCompleted)"
                stroke="var(--color-completed)"
                strokeWidth={2}
                isAnimationActive={!reduceMotion}
              />
              {showFailed && (
                <Area
                  dataKey="failed"
                  name="Провалено"
                  type="monotone"
                  stackId="trend"
                  fill="url(#trendFailed)"
                  stroke="var(--color-failed)"
                  strokeWidth={2}
                  isAnimationActive={!reduceMotion}
                />
              )}
            </AreaChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  );
}
