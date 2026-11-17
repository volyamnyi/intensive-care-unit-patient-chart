import { describe, expect, it } from "vitest";

import {
  composeActualTime,
  formatActualTime,
  normalizeTime,
  validateActualTime,
} from "./actualTime";

describe("normalizeTime", () => {
  it("pads HH:mm to HH:mm:00", () => {
    expect(normalizeTime("10:30")).toBe("10:30:00");
  });

  it("keeps HH:mm:ss as-is", () => {
    expect(normalizeTime("10:30:07")).toBe("10:30:07");
  });

  it("returns empty string for garbage", () => {
    expect(normalizeTime("abc")).toBe("");
    expect(normalizeTime("")).toBe("");
    expect(normalizeTime(null)).toBe("");
    expect(normalizeTime(undefined)).toBe("");
  });
});

describe("composeActualTime", () => {
  it("composes when both parts present", () => {
    expect(composeActualTime("2026-11-17", "10:30")).toBe("2026-11-17T10:30:00");
    expect(composeActualTime("2026-11-17", "10:30:07")).toBe("2026-11-17T10:30:07");
  });

  it("returns null when either part missing", () => {
    expect(composeActualTime(null, "10:30:00")).toBeNull();
    expect(composeActualTime("2026-11-17", null)).toBeNull();
    expect(composeActualTime("", "10:30:00")).toBeNull();
    expect(composeActualTime("2026-11-17", "")).toBeNull();
  });

  it("returns null for invalid date", () => {
    expect(composeActualTime("not-a-date", "10:30:00")).toBeNull();
  });
});

describe("formatActualTime", () => {
  it("formats ISO to dd.MM.yyyy HH:mm:ss", () => {
    expect(formatActualTime("2026-11-17T10:30:07")).toBe("17.11.2026 10:30:07");
  });

  it("returns empty for null/invalid", () => {
    expect(formatActualTime(null)).toBe("");
    expect(formatActualTime("garbage")).toBe("");
  });

  it("formats a HH:mm value with zero seconds", () => {
    expect(formatActualTime("2026-11-17T10:30")).toBe("17.11.2026 10:30:00");
    expect(formatActualTime("2026-11-17T10:30:00")).toBe("17.11.2026 10:30:00");
  });

  it("keeps the wall clock on a DST spring-forward day (#447)", () => {
    // 2026-03-29 03:00→04:00 in Europe/Kiev: 03:30 does not exist there.
    // Routing the value through Date would shift the recorded fact to 04:30.
    expect(formatActualTime("2026-03-29T03:30:00")).toBe("29.03.2026 03:30:00");
    expect(formatActualTime("2026-03-29T00:00:30")).toBe("29.03.2026 00:00:30");
  });

  it("keeps the wall clock on a DST fall-back day (#447)", () => {
    expect(formatActualTime("2026-10-25T03:30:00")).toBe("25.10.2026 03:30:00");
  });

  it("returns empty for an impossible calendar date", () => {
    expect(formatActualTime("2026-02-30T10:30:00")).toBe("");
  });
});

describe("validateActualTime", () => {
  const now = "2026-11-17T12:00:00";
  const min = "2026-11-10";
  const max = "2026-11-17";

  it("OK for a value within tolerance (now + 5 min inclusive)", () => {
    expect(validateActualTime("2026-11-17T12:05:00", { nowIso: now, minDate: min, maxDate: max })).toBe("OK");
  });

  it("FUTURE when beyond tolerance", () => {
    expect(validateActualTime("2026-11-17T12:05:01", { nowIso: now, minDate: min, maxDate: max })).toBe("FUTURE");
    expect(validateActualTime("2030-01-01T00:00:00", { nowIso: now, minDate: min, maxDate: max })).toBe("FUTURE");
  });

  it("MALFORMED for unparseable input", () => {
    expect(validateActualTime("not-a-date", { nowIso: now })).toBe("MALFORMED");
    expect(validateActualTime(null, { nowIso: now })).toBe("MALFORMED");
  });

  it("MALFORMED for an impossible calendar date", () => {
    expect(validateActualTime("2026-02-30T10:30:00", { nowIso: now })).toBe("MALFORMED");
  });

  it("BEFORE_MIN at lower bound exclusive", () => {
    expect(validateActualTime("2026-11-09T23:59:59", { minDate: min, maxDate: max })).toBe("BEFORE_MIN");
  });

  it("inclusive lower bound is OK", () => {
    expect(validateActualTime("2026-11-10T00:00:00", { minDate: min, maxDate: max })).toBe("OK");
  });

  it("AFTER_MAX beyond max+1", () => {
    expect(validateActualTime("2026-11-19T00:00:00", { minDate: min, maxDate: max })).toBe("AFTER_MAX");
  });

  it("inclusive upper bound (max+1 day) is OK (night dose)", () => {
    expect(validateActualTime("2026-11-18T23:59:59", { minDate: min, maxDate: max })).toBe("OK");
    expect(validateActualTime("2026-11-18T08:00:00", { minDate: min, maxDate: max })).toBe("OK");
  });

  it("DISPENSE_AFTER_ADMIN when dispensed later", () => {
    expect(
      validateActualTime("2026-11-17T10:30:00", {
        minDate: min,
        maxDate: max,
        dispensedIso: "2026-11-17T10:30:01",
      }),
    ).toBe("DISPENSE_AFTER_ADMIN");
  });

  it("equal dispensed/administered is OK", () => {
    expect(
      validateActualTime("2026-11-17T10:30:00", {
        minDate: min,
        maxDate: max,
        dispensedIso: "2026-11-17T10:30:00",
      }),
    ).toBe("OK");
  });

  it("accepts the first seconds of the day after the cell date (#447)", () => {
    // The night dose of day D may land in the morning of D+1 - the window
    // starts at 00:00:00 of that day.
    expect(
      validateActualTime("2026-11-18T00:00:30", {
        minDate: min,
        maxDate: max,
        nowIso: "2026-11-18T00:05:00",
      }),
    ).toBe("OK");
  });

  it("compares on the wall clock, not on an instant (#447)", () => {
    // On the DST day 03:00→04:00 in Europe/Kiev the local times 03:00-03:59
    // do not exist. An instant-based comparison would move 03:02 to 04:02 and
    // report a 64-minute gap instead of the real 4 minutes.
    expect(
      validateActualTime("2026-03-29T03:02:00", {
        minDate: "2026-03-29",
        maxDate: "2026-03-29",
        nowIso: "2026-03-29T02:58:00",
      }),
    ).toBe("OK");
    // Beyond the 5-minute tolerance on the wall clock -> FUTURE.
    expect(
      validateActualTime("2026-03-29T03:06:00", {
        minDate: "2026-03-29",
        maxDate: "2026-03-29",
        nowIso: "2026-03-29T02:58:00",
      }),
    ).toBe("FUTURE");
    // An equal value is never in the future.
    expect(
      validateActualTime("2026-03-29T03:30:00", {
        minDate: "2026-03-29",
        maxDate: "2026-03-29",
        nowIso: "2026-03-29T03:30:00",
      }),
    ).toBe("OK");
  });

  it("keeps the V3 window on a DST day (#447)", () => {
    // The list starts on the DST day; the lower bound stays 00:00 of that day.
    expect(
      validateActualTime("2026-03-29T00:00:30", {
        minDate: "2026-03-29",
        maxDate: "2026-03-29",
        nowIso: "2026-03-29T12:00:00",
      }),
    ).toBe("OK");
    expect(
      validateActualTime("2026-03-28T23:59:59", {
        minDate: "2026-03-29",
        maxDate: "2026-03-29",
        nowIso: "2026-03-29T12:00:00",
      }),
    ).toBe("BEFORE_MIN");
    expect(
      validateActualTime("2026-03-30T00:00:00", {
        minDate: "2026-03-29",
        maxDate: "2026-03-29",
        nowIso: "2026-03-30T12:00:00",
      }),
    ).toBe("OK");
    expect(
      validateActualTime("2026-03-31T00:00:00", {
        minDate: "2026-03-29",
        maxDate: "2026-03-29",
        nowIso: "2026-03-31T12:00:00",
      }),
    ).toBe("AFTER_MAX");
  });
});
