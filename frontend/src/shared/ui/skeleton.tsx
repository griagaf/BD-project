import { cn } from "@/shared/lib/cn"

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn("animate-pulse rounded-md bg-zinc-800/70", className)} />
}

export function TableSkeleton({ rows = 6, columns = 5 }: { rows?: number; columns?: number }) {
  return (
    <div className="overflow-hidden rounded-md border border-zinc-800 bg-zinc-950/80">
      <div className="grid gap-px bg-zinc-900/80 p-px" style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }}>
        {Array.from({ length: columns }).map((_, index) => (
          <div key={`head-${index}`} className="bg-zinc-900 p-3">
            <Skeleton className="h-4 w-2/3" />
          </div>
        ))}
        {Array.from({ length: rows * columns }).map((_, index) => (
          <div key={index} className="bg-zinc-950 p-3">
            <Skeleton className="h-4 w-full" />
          </div>
        ))}
      </div>
    </div>
  )
}
