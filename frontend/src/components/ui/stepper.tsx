/* eslint-disable react/only-export-components */

"use client"

import * as React from "react"
import { Check, Loader2 } from "lucide-react"

import { cn } from "@/lib/utils"

type StepperOrientation = "horizontal" | "vertical"
type StepperSize = "sm" | "md" | "lg"
type StepState = "active" | "completed" | "inactive"

interface StepperContextValue {
  orientation: StepperOrientation
  size: StepperSize
  nonLinear: boolean
  onStepClick?: (step: number) => void
}

const StepperContext = React.createContext<StepperContextValue | null>(null)

function useStepper(): StepperContextValue {
  const ctx = React.useContext(StepperContext)
  if (!ctx) {
    throw new Error("useStepper must be used within <Stepper>")
  }
  return ctx
}

interface StepperProps
  extends Omit<React.OlHTMLAttributes<HTMLOListElement>, "children"> {
  /** Currently active step, 1-based. */
  step?: number
  orientation?: StepperOrientation
  size?: StepperSize
  /** Allow navigating to any step (otherwise steps are display-only). */
  nonLinear?: boolean
  onStepClick?: (step: number) => void
  children?: React.ReactNode
}

const Stepper = React.forwardRef<HTMLOListElement, StepperProps>(
  function Stepper(
    {
      step = 1,
      orientation = "horizontal",
      size = "md",
      nonLinear = false,
      onStepClick,
      className,
      children,
      ...props
    },
    ref,
  ) {
    const steps = React.Children.toArray(children).filter(React.isValidElement)

    return (
      <StepperContext.Provider value={{ orientation, size, nonLinear, onStepClick }}>
        <ol
          ref={ref}
          data-slot="stepper"
          className={cn(
            "flex items-center gap-2",
            orientation === "vertical" && "flex-col items-stretch gap-0",
            className,
          )}
          {...props}
        >
          {steps.map((child, i) =>
            React.cloneElement(child as React.ReactElement<StepProps>, {
              index: i + 1,
              isActive: i + 1 === step,
              isCompleted: i + 1 < step,
              isLast: i + 1 === steps.length,
            }),
          )}
        </ol>
      </StepperContext.Provider>
    )
  },
)

interface StepProps
  extends Omit<React.LiHTMLAttributes<HTMLLIElement>, "children" | "title"> {
  index?: number
  isActive?: boolean
  isCompleted?: boolean
  isLast?: boolean
  title?: React.ReactNode
  description?: React.ReactNode
  icon?: React.ReactNode
  loading?: boolean
  children?: React.ReactNode
}

const Step = React.forwardRef<HTMLLIElement, StepProps>(function Step(
  {
    index = 1,
    isActive = false,
    isCompleted = false,
    isLast = false,
    title,
    description,
    icon,
    loading = false,
    className,
    children,
    ...props
  },
  ref,
) {
  const { orientation } = useStepper()
  const state: StepState = isCompleted ? "completed" : isActive ? "active" : "inactive"

  return (
    <li
      ref={ref}
      data-slot="stepper-step"
      data-state={state}
      className={cn(
        "flex",
        orientation === "horizontal" ? "items-center gap-2" : "items-start gap-3",
        className,
      )}
      {...props}
    >
      {children ?? (
        <>
          <div
            className={cn(
              orientation === "horizontal"
                ? "contents"
                : "flex flex-col items-center self-stretch",
            )}
          >
            <StepIndicator index={index} icon={icon} loading={loading} state={state} />
            {orientation === "vertical" && (
              <StepSeparator state={state} hidden={isLast} vertical />
            )}
          </div>
          <div className="min-w-0">
            {title !== undefined && <StepTitle state={state}>{title}</StepTitle>}
            {description !== undefined && <StepDescription>{description}</StepDescription>}
          </div>
          {orientation === "horizontal" && (
            <StepSeparator state={state} hidden={isLast} />
          )}
        </>
      )}
    </li>
  )
})

interface StepIndicatorProps {
  index: number
  state: StepState
  icon?: React.ReactNode
  loading: boolean
}

const StepIndicator = React.forwardRef<HTMLSpanElement, StepIndicatorProps>(
  function StepIndicator({ index, state, icon, loading }, ref) {
    const { size, nonLinear, onStepClick } = useStepper()
    const interactive = nonLinear && onStepClick !== undefined

    const sizeClasses: Record<StepperSize, string> = {
      sm: "h-6 w-6 text-xs",
      md: "h-8 w-8 text-sm",
      lg: "h-10 w-10 text-base",
    }

    const iconSizeClasses: Record<StepperSize, string> = {
      sm: "size-3.5",
      md: "size-4",
      lg: "size-5",
    }

    return (
      <span
        ref={ref}
        data-slot="stepper-indicator"
        data-state={state}
        aria-current={state === "active" ? "step" : undefined}
        role={interactive ? "button" : undefined}
        tabIndex={interactive ? 0 : undefined}
        onClick={interactive ? () => onStepClick?.(index) : undefined}
        onKeyDown={
          interactive
            ? (event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault()
                  onStepClick?.(index)
                }
              }
            : undefined
        }
        className={cn(
          "flex shrink-0 items-center justify-center rounded-full border font-medium transition-colors",
          sizeClasses[size],
          state === "active" && "border-primary bg-background text-primary ring-2 ring-primary/20",
          state === "completed" && "border-primary bg-primary text-primary-foreground",
          state === "inactive" && "border-border text-muted-foreground",
          interactive && "cursor-pointer active:translate-y-px",
        )}
      >
        {loading ? (
          <Loader2
            data-slot="stepper-loading"
            aria-hidden="true"
            className={cn("animate-spin", iconSizeClasses[size])}
          />
        ) : state === "completed" && icon === undefined ? (
          <Check
            data-slot="stepper-check"
            aria-hidden="true"
            className={iconSizeClasses[size]}
          />
        ) : icon !== undefined ? (
          icon
        ) : (
          <span className="tabular-nums">{index}</span>
        )}
      </span>
    )
  },
)

function StepSeparator({
  state,
  hidden,
  vertical,
}: {
  state: StepState
  hidden?: boolean
  vertical?: boolean
}) {
  return (
    <span
      data-slot="stepper-separator"
      data-state={state}
      aria-hidden="true"
      className={cn(
        "bg-border",
        vertical ? "w-px flex-1" : "h-px min-w-4 flex-1",
        state === "completed" && "bg-primary",
        hidden && "hidden",
      )}
    />
  )
}

const StepTitle = React.forwardRef<
  HTMLSpanElement,
  React.HTMLAttributes<HTMLSpanElement> & { state?: StepState }
>(function StepTitle({ state, className, ...props }, ref) {
  return (
    <span
      ref={ref}
      data-slot="stepper-title"
      data-state={state}
      className={cn(
        "block text-sm font-medium",
        state === "inactive" ? "text-muted-foreground" : "text-foreground",
        className,
      )}
      {...props}
    />
  )
})

const StepDescription = React.forwardRef<
  HTMLSpanElement,
  React.HTMLAttributes<HTMLSpanElement>
>(function StepDescription({ className, ...props }, ref) {
  return (
    <span
      ref={ref}
      data-slot="stepper-description"
      className={cn("block text-xs text-muted-foreground", className)}
      {...props}
    />
  )
})

export { Stepper, Step, StepIndicator, StepSeparator, StepTitle, StepDescription, useStepper }