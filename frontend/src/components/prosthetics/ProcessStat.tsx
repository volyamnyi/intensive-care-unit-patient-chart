export function ProcessStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border bg-card p-4 text-center">
      <div className="font-display text-xl font-semibold">{value}</div>
      <div className="text-xs text-muted-foreground">{label}</div>
    </div>
  );
}