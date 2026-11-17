import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";

import { DatetimeInput } from "./datetime-input";

describe("DatetimeInput", () => {
  it("renders two labelled native inputs", () => {
    render(
      <DatetimeInput
        value={null}
        onChange={() => {}}
        dateAriaLabel="Дата"
        timeAriaLabel="Час"
      />,
    );
    expect(screen.getByLabelText("Дата")).toHaveAttribute("type", "date");
    expect(screen.getByLabelText("Час")).toHaveAttribute("type", "time");
  });

  it("reflects the controlled value", () => {
    render(
      <DatetimeInput
        value="2026-11-17T10:30:07"
        onChange={() => {}}
        dateAriaLabel="Дата"
        timeAriaLabel="Час"
      />,
    );
    expect(screen.getByLabelText("Дата")).toHaveValue("2026-11-17");
    expect(screen.getByLabelText("Час")).toHaveValue("10:30:07");
  });

  it("emits ISO when both parts are filled (seconds normalized)", () => {
    const onChange = vi.fn();
    render(
      <DatetimeInput
        value={null}
        onChange={onChange}
        dateAriaLabel="Дата"
        timeAriaLabel="Час"
      />,
    );
    fireEvent.change(screen.getByLabelText("Дата"), { target: { value: "2026-11-17" } });
    fireEvent.change(screen.getByLabelText("Час"), { target: { value: "10:30" } });
    expect(onChange).toHaveBeenLastCalledWith("2026-11-17T10:30:00");
  });

  it("emits null when a part is cleared", () => {
    const onChange = vi.fn();
    render(
      <DatetimeInput
        value="2026-11-17T10:30:07"
        onChange={onChange}
        dateAriaLabel="Дата"
        timeAriaLabel="Час"
      />,
    );
    fireEvent.change(screen.getByLabelText("Дата"), { target: { value: "" } });
    expect(onChange).toHaveBeenLastCalledWith(null);
  });

  it("disabled blocks both fields", () => {
    render(
      <DatetimeInput
        value="2026-11-17T10:30:07"
        onChange={() => {}}
        disabled
        dateAriaLabel="Дата"
        timeAriaLabel="Час"
      />,
    );
    expect(screen.getByLabelText("Дата")).toBeDisabled();
    expect(screen.getByLabelText("Час")).toBeDisabled();
  });

  it("invalid sets aria-invalid on both fields", () => {
    render(
      <DatetimeInput
        value="2026-11-17T10:30:07"
        onChange={() => {}}
        invalid
        dateAriaLabel="Дата"
        timeAriaLabel="Час"
      />,
    );
    expect(screen.getByLabelText("Дата")).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText("Час")).toHaveAttribute("aria-invalid", "true");
  });

  it("time input carries step=1", () => {
    render(
      <DatetimeInput
        value={null}
        onChange={() => {}}
        dateAriaLabel="Дата"
        timeAriaLabel="Час"
      />,
    );
    expect(screen.getByLabelText("Час")).toHaveAttribute("step", "1");
  });
});
