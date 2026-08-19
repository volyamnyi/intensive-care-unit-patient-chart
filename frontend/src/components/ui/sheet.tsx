"use client"

import * as React from "react"
import { Drawer as DrawerPrimitive } from "@base-ui/react"
import { XIcon } from "lucide-react"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"

function Sheet({ swipeDirection = "down", ...props }: DrawerPrimitive.Root.Props) {
  return (
    <DrawerPrimitive.Root data-slot="sheet" swipeDirection={swipeDirection} {...props} />
  )
}

const SheetTrigger = React.forwardRef<
  HTMLButtonElement,
  DrawerPrimitive.Trigger.Props
>(function SheetTrigger({ className, ...props }, ref) {
  return (
    <DrawerPrimitive.Trigger
      ref={ref}
      data-slot="sheet-trigger"
      className={cn(className)}
      {...props}
    />
  )
})

const SheetClose = React.forwardRef<
  HTMLButtonElement,
  DrawerPrimitive.Close.Props
>(function SheetClose({ className, ...props }, ref) {
  return (
    <DrawerPrimitive.Close
      ref={ref}
      data-slot="sheet-close"
      className={cn(className)}
      {...props}
    />
  )
})

const SheetPortal = DrawerPrimitive.Portal

function SheetOverlay({
  className,
  ...props
}: DrawerPrimitive.Backdrop.Props) {
  return (
    <DrawerPrimitive.Backdrop
      data-slot="sheet-overlay"
      className={cn("fixed inset-0 z-50 bg-black/50", className)}
      {...props}
    />
  )
}

const SheetContent = React.forwardRef<
  HTMLDivElement,
  DrawerPrimitive.Popup.Props & {
    side?: "top" | "right" | "bottom" | "left"
  }
>(function SheetContent({ className, children, side = "right", ...props }, ref) {
  const sideClasses: Record<string, string> = {
    top: "inset-x-0 top-0 h-auto border-b pt-[max(1.5rem,env(safe-area-inset-top))]",
    right: "inset-y-0 right-0 h-full w-3/4 border-l pr-[max(1.5rem,env(safe-area-inset-right))] sm:max-w-sm",
    bottom: "inset-x-0 bottom-0 h-auto border-t pb-[max(1.5rem,env(safe-area-inset-bottom))]",
    left: "inset-y-0 left-0 h-full w-3/4 border-r pl-[max(1.5rem,env(safe-area-inset-left))] sm:max-w-sm",
  }

  return (
    <SheetPortal>
      <SheetOverlay />
      <DrawerPrimitive.Popup
        ref={ref}
        data-slot="sheet-content"
        data-side={side}
        className={cn(
          "fixed z-50 flex flex-col gap-4 bg-popover p-6 text-popover-foreground shadow-lg outline-none",
          sideClasses[side],
          className,
        )}
        {...props}
      >
        {children}
        <DrawerPrimitive.Close
          render={
            <Button
              variant="ghost"
              size="icon-sm"
              className="absolute top-2 right-2"
            />
          }
        >
          <XIcon className="size-4" />
          <span className="sr-only">Close</span>
        </DrawerPrimitive.Close>
      </DrawerPrimitive.Popup>
    </SheetPortal>
  )
})

function SheetHeader({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      data-slot="sheet-header"
      className={cn("flex flex-col gap-1.5", className)}
      {...props}
    />
  )
}

function SheetFooter({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      data-slot="sheet-footer"
      className={cn("mt-auto flex flex-col gap-2", className)}
      {...props}
    />
  )
}

const SheetTitle = React.forwardRef<
  HTMLHeadingElement,
  DrawerPrimitive.Title.Props
>(function SheetTitle({ className, ...props }, ref) {
  return (
    <DrawerPrimitive.Title
      ref={ref}
      data-slot="sheet-title"
      className={cn("text-foreground font-semibold", className)}
      {...props}
    />
  )
})

const SheetDescription = React.forwardRef<
  HTMLParagraphElement,
  DrawerPrimitive.Description.Props
>(function SheetDescription({ className, ...props }, ref) {
  return (
    <DrawerPrimitive.Description
      ref={ref}
      data-slot="sheet-description"
      className={cn("text-muted-foreground text-sm", className)}
      {...props}
    />
  )
})

export {
  Sheet,
  SheetTrigger,
  SheetClose,
  SheetPortal,
  SheetOverlay,
  SheetContent,
  SheetHeader,
  SheetFooter,
  SheetTitle,
  SheetDescription,
}
