"use client"

import * as React from "react"

export function useMediaQuery(query: string): boolean {
  const subscribe = React.useCallback(
    (callback: () => void) => {
      const mediaQueryList = window.matchMedia(query)
      mediaQueryList.addEventListener("change", callback)
      return () => mediaQueryList.removeEventListener("change", callback)
    },
    [query],
  )

  return React.useSyncExternalStore(
    subscribe,
    () => window.matchMedia(query).matches,
    () => false,
  )
}

/** True when the viewport is below the mobile breakpoint (< 640px). */
export function useIsMobile(): boolean {
  return useMediaQuery("(max-width: 639.98px)")
}