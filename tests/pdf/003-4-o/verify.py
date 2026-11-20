"""Independent reader for the two *standard-height* 003-4/о reference datasets.

Coordinates here are the measured print specification (points, origin at top left),
not coordinates supplied by a renderer. Never write original patient or staff text.
"""

import argparse
from bisect import bisect_right
from collections import defaultdict
from ctypes import byref
from datetime import date, timedelta
from hashlib import sha256
import json
from math import ceil, floor, hypot
from pathlib import Path

import pypdfium2 as pdfium
import pypdfium2.raw as pdfium_raw
from PIL import Image, ImageDraw, ImageFont, ImageStat


HERE = Path(__file__).resolve().parent
LEFT, RIGHT = 15.25, 827.00
UNIT = (RIGHT - LEFT) / 1620
XS = [LEFT + n * UNIT for n in [0, 250, 315, *(360 + 45 * i for i in range(29))]]
PERIODS = ("Ранок", "День", "Вечір", "Ніч")

# Independent acceptance values from the specification, not calculated from the fixtures.
# Per physical page: week offset, occupied pairs, upper marks, lower marks, signed nurse slots.
CASES = {
    1: (date(2026, 10, 16), "e1d025abc604864440ccf95cbf4c3c02714b39554b1ce5dbc75bc6079017fe5a",
        [(0, 12, 107, 105, 23), (0, 5, 26, 26, 13), (1, 12, 148, 145, 28),
         (1, 6, 40, 38, 21), (2, 12, 119, 116, 25), (3, 8, 43, 5, 1)]),
    2: (date(2026, 10, 20), "7bc345fea985673e3e012dd671376f6f3ebbfdae03296be19200ba4adc308ce9",
        [(0, 12, 114, 99, 22), (0, 1, 1, 1, 1), (1, 12, 134, 129, 28),
         (1, 2, 31, 31, 28), (2, 12, 153, 151, 28), (2, 2, 3, 3, 2),
         (3, 8, 64, 40, 12), (4, 2, 12, 0, 0)]),
}


class Mismatch(ValueError):
    pass


def require(condition, message):
    if not condition:
        raise Mismatch(message)


def raster_line(image, expected, start, end, vertical):
    """Locate the ink centroid across a ruled line at 144 dpi, allowing antialiasing."""
    a, b = round(start * 2) + 1, round(end * 2) - 1
    pixels = []
    for i in range(round(expected * 2) - 4, round(expected * 2) + 5):
        box = (i, a, i + 1, b) if vertical else (a, i, b, i + 1)
        ink = 1 - ImageStat.Stat(image.crop(box)).mean[0] / 255
        pixels.append(((i + 0.5) / 2, ink))
    peak = max(ink for _, ink in pixels)
    axis = "x" if vertical else "y"
    require(peak >= 0.25, f"missing ruled line at {axis}={expected:.2f} pt")
    stripe = [(p, w) for p, w in pixels if w >= max(0.16, peak * 0.45)]
    actual = sum(p * w for p, w in stripe) / sum(w for _, w in stripe)
    require(abs(actual - expected) <= 1, f"line at {axis}={actual:.2f}; expected {expected:.2f} ±1 pt")
    return actual


def check_raster(page, top):
    require(page.get_rotation() == 0, "page rotation differs")
    require(all(abs(a - b) <= 1 for a, b in zip(page.get_mediabox(), (0, 0, 841.92, 594.96))),
            "MediaBox differs")
    require(all(abs(a - b) <= 1 for a, b in zip(page.get_size(), (841.92, 594.96))), "cropped page")
    bitmap = page.render(scale=2)
    try:
        image = bitmap.to_pil().convert("L")
    finally:
        bitmap.close()
    ys = lambda n: top + n * UNIT
    xs = [raster_line(image, x, ys(320), ys(1040), True) for x in XS]
    # Internal shared rules have ink on both sides. The reference's single
    # 0.5 pt outside border is drawn *inside* the last column's outer edge.
    xs[-1] += 0.25
    for i in range(3, 31):
        require(abs(xs[i + 1] - xs[i] - 45 * UNIT) <= 0.5, f"slot {i - 2}: pitch outside ±0.5 pt")
    for y in [0, 165, 225, 255, 275, 300, *range(320, 1041, 30), 1060, 1080]:
        raster_line(image, ys(y), XS[3], RIGHT, False)
    for pos, y1, y2 in [(730, 0, 165), (1080, 0, 165), (390, 165, 225), (1270, 165, 225)]:
        raster_line(image, LEFT + pos * UNIT, ys(y1), ys(y2), True)
    return image


def has_ink(image, x0, x1, y0, y1):
    # Ignore only the ruled border + antialiasing around it. Detect both
    # invisible PDF text and stray visible ink in a nominally empty cell.
    bounds = (ceil(2 * x0) + 2, ceil(2 * y0) + 2, floor(2 * x1) - 2, floor(2 * y1) - 2)
    return image.crop(bounds).getextrema()[0] < 160


def page_cells(page, top, original):
    """Assign glyphs by their ink centers, not text-box intersection with adjacent rows."""
    fields = defaultdict(str)
    marks = bytearray(12 * 2 * 28)
    text = page.get_textpage()
    try:
        for i in range(text.count_chars()):
            char = chr(pdfium_raw.FPDFText_GetUnicode(text, i))
            if char.isspace() or ord(char) < 32:
                continue
            left, bottom, right, upper = text.get_charbox(i)
            y = (page.get_height() - (upper + bottom) / 2 - top) / UNIT
            col = bisect_right(XS, (left + right) / 2) - 1
            if not (0 <= col < 31 and 0 <= y < 1080):
                continue
            if 165 <= y < 225:
                x = ((left + right) / 2 - LEFT) / UNIT
                key = ("patient", bisect_right((390, 1270), x))
            elif y < 255:
                key = ("header",)
            elif y < 300:
                key = ("date" if y < 275 else "period", col)
            elif y < 320:
                key = ("regime", col)
            elif y < 1040:
                half = int((y - 320) // 30)
                if col >= 3:
                    pos = half * 28 + col - 3
                    where = f"pair {half // 2 + 1}, role {half % 2 + 1}, slot {col - 2}"
                    require(char == "+" and not marks[pos], f"{where}: unexpected or doubled mark")
                    row_top = top + (320 + half * 30) * UNIT
                    require(left >= XS[col] and right <= XS[col + 1]
                            and page.get_height() - upper >= row_top
                            and page.get_height() - bottom <= row_top + 30 * UNIT, f"{where}: clipped mark")
                    marks[pos] = 1
                    continue
                key = ("name", half // 2) if col == 0 else ("role" if col == 1 else "spare", half)
            else:
                key = ("footer", int(y >= 1060), col)
                if col >= 3 and not original:
                    matrix = pdfium_raw.FS_MATRIX()
                    require(pdfium_raw.FPDFText_GetMatrix(text, i, byref(matrix)), "missing font transform")
                    size = pdfium_raw.FPDFText_GetFontSize(text, i) * hypot(matrix.c, matrix.d)
                    require(size >= 4.999, f"slot {col - 2}: signature smaller than 5 pt")
                    require(left >= XS[col] and right <= XS[col + 1], f"slot {col - 2}: signature overflow")
            fields[key] += char
    finally:
        text.close()
    return fields, marks


def synthetic_label(item_id, length):
    label = f"Тестовий препарат {item_id}"
    for word in (" умовна", " назва", " тестової", " позиції") * 6:
        if len((label + word).replace(" ", "")) > length:
            break
        label += word
    return label


def save_grid(image, top, nurses, path):
    """Only the verified, non-identifying time grid; the footer is freshly typeset from aliases."""
    left, y = round(XS[3] * 2), round((top + 255 * UNIT) * 2)
    bottom = round((top + 1040 * UNIT) * 2)
    grid = Image.new("L", (round(RIGHT * 2) - left, round((top + 1080 * UNIT) * 2) - y), 255)
    grid.paste(image.crop((left, y, left + grid.width, bottom)), (0, 0))
    draw = ImageDraw.Draw(grid)
    font = ImageFont.truetype(str(HERE.parents[2] / "backend/common/src/main/resources/fonts/DejaVuSans.ttf"), 10)
    mid = round((top + 1060 * UNIT) * 2) - y
    for edge in XS[3:]:
        x = min(grid.width - 1, round(edge * 2) - left)
        draw.line((x, bottom - y, x, grid.height - 1), fill=0)
    for line in (bottom - y, mid, grid.height - 1):
        draw.line((0, line, grid.width - 1, line), fill=0)
    for slot, alias in enumerate(nurses):
        if alias != ".":
            draw.text((XS[slot + 3] + XS[slot + 4] - left, (mid + grid.height) / 2), alias,
                      font=font, anchor="mm", fill=0)
    path.parent.mkdir(parents=True, exist_ok=True)
    grid.save(path, dpi=(144, 144))


def inspect_document(path, example, original=False):
    start, digest, spec = CASES[example]
    expected = None if original else json.loads((HERE / f"example{example}.json").read_text("utf-8"))
    # Raw names are temporary dictionary keys only. Never include them in diagnostics or fixtures.
    names, logins, medicines, pages = {}, {}, {}, []
    if expected:
        names = {"".join(name.split()): key for key, name in expected["medicines"].items()}
    bits = bytearray()
    with pdfium.PdfDocument(path) as doc:
        require(len(doc) == len(spec), f"expected {len(spec)} physical pages, found {len(doc)}")
        for p, (week, occupied, planned, done, signed) in enumerate(spec):
            page = doc[p]
            try:
                top = 30.29 if p == 0 else 15.25
                image = check_raster(page, top)
                cells, marks = page_cells(page, top, original)
                for label in ("ЄДРПОУ", "МЕДИЧНАДОКУМЕНТАЦІЯ", "003-4/о", "29.05.2013",
                              "435", "Відміткапропризначеннятавиконання"):
                    require(label in cells["header",], f"missing repeated header label: {label}")
                require("ЛИСТОКЛІКАРСЬКИХПРИЗНАЧЕНЬ" in cells["patient", 1], "missing sheet title")
                if not original:
                    require("Синтетичнийпацієнт" in cells["patient", 1], "patient field differs")
                    require("Тестовийзаклад" in cells["header",] and "00123456" in cells["header",],
                            "institution or leading zeroes lost")
                    require(not any(c.isdigit() for c in cells["patient", 0] + cells["patient", 2]),
                            "unverified card or room number")
                require(cells["regime", 0] == "Режим" and all(not cells["regime", c] for c in range(1, 31)),
                        "general regime must be blank for this dataset")
                week_start = start + timedelta(weeks=week)
                nurses, rows = [], []
                for slot in range(28):
                    col = 3 + slot
                    day = week_start + timedelta(days=slot // 4)
                    require(cells["date", col] == day.strftime("%d.%m.%y")
                            and cells["period", col] == PERIODS[slot % 4], f"slot {slot + 1}: date/period")
                    require(not cells["footer", 0, col], f"slot {slot + 1}: unexpected doctor signature")
                    nurse = cells["footer", 1, col]
                    for role, filled in enumerate([False, bool(nurse)]):
                        y0 = top + (1040 + role * 20) * UNIT
                        require(has_ink(image, XS[col], XS[col + 1], y0, y0 + 20 * UNIT) == filled,
                                f"slot {slot + 1}: signature ink differs from text layer")
                    if nurse:
                        require(any(marks[28 + slot::56]), f"slot {slot + 1}: signature without a fact")
                        if original:
                            nurse = logins.setdefault(nurse, f"n{len(logins) + 1:02}")
                        else:
                            require(nurse == expected["pages"][p]["nurses"].split()[slot],
                                    f"slot {slot + 1}: incorrect executor")
                    nurses.append(nurse or ".")
                require(sum(n != "." for n in nurses) == signed, "signed-slot count differs")
                require(cells["footer", 0, 1] == "Лікар" and cells["footer", 1, 1] == "Сестра"
                        and "Підписи" in cells["footer", 0, 0] + cells["footer", 1, 0], "signature rows")
                for row in range(12):
                    for half, role in [(row * 2, "Лікар"), (row * 2 + 1, "Сестра")]:
                        require(cells["role", half] == role and not cells["spare", half],
                                f"pair {row + 1}: role/spare column")
                        for slot in range(28):
                            y0 = top + (320 + 30 * half) * UNIT
                            ink = has_ink(image, XS[3 + slot], XS[4 + slot], y0, y0 + 30 * UNIT)
                            require(ink == bool(marks[half * 28 + slot]),
                                    f"pair {row + 1}, {role}, slot {slot + 1}: ink differs from text layer")
                    name = cells["name", row]
                    pair = marks[56 * row:56 * (row + 1)]
                    require(bool(name) == bool(any(pair)) == (row < occupied), f"pair {row + 1}: occupancy")
                    if not name:
                        continue
                    if original:
                        medicine = names.setdefault(name, f"M{len(names) + 1:02}")
                        medicines[medicine] = synthetic_label(medicine, len(name))
                    else:
                        require(name in names, f"pair {row + 1}: incomplete or unexpected synthetic name")
                        medicine = names[name]
                    rows.append(f"{medicine} {''.join(map(str, pair[:28]))} {''.join(map(str, pair[28:]))}")
                require(sum(sum(marks[r * 56:r * 56 + 28]) for r in range(12)) == planned
                        and sum(marks) == planned + done, "clinical mark counts differ")
                result = {"start": str(week_start), "rows": rows, "nurses": " ".join(nurses)}
                if expected:
                    require(result == expected["pages"][p], "item order, per-slot marks or signatures differ")
                pages.append(result)
                bits.extend(marks)
            except Mismatch as error:
                raise Mismatch(f"page {p + 1}: {error}") from error
            finally:
                page.close()
    require(sha256(bits).hexdigest() == digest, "clinical mark matrix SHA-256 differs")
    return {"medicines": medicines if original else expected["medicines"], "pages": pages}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("example", type=int, choices=CASES)
    parser.add_argument("pdf", type=Path)
    parser.add_argument("--original", action="store_true", help="check original instead of synthetic names")
    parser.add_argument("--extract", type=Path, metavar="JSON", help="save anonymous fixture from original")
    parser.add_argument("--grid-dir", type=Path, help="export reference time-grid crops with anonymized footers")
    args = parser.parse_args()
    if (args.extract or args.grid_dir) and not args.original:
        parser.error("--extract and --grid-dir require --original")
    fixture = inspect_document(args.pdf, args.example, args.original)
    if args.extract:
        args.extract.write_text(json.dumps(fixture, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    else:
        saved = json.loads((HERE / f"example{args.example}.json").read_text("utf-8"))
        require(saved == fixture, "document differs from committed fixture")
    if args.grid_dir:
        with pdfium.PdfDocument(args.pdf) as doc:
            for p, data in enumerate(fixture["pages"]):
                page = doc[p]
                bitmap = page.render(scale=2)
                try:
                    save_grid(bitmap.to_pil().convert("L"), 30.29 if p == 0 else 15.25,
                              data["nurses"].split(), args.grid_dir / f"example{args.example}-p{p + 1:02}.png")
                finally:
                    bitmap.close()
                    page.close()
    pages = CASES[args.example][2]
    print(f"{len(pages)} pages, {len(fixture['medicines'])} medicines, "
          f"{sum(p[2] + p[3] for p in pages)} marks, 144 dpi; SHA-256 {CASES[args.example][1]}")


if __name__ == "__main__":
    try:
        main()
    except Mismatch as error:
        raise SystemExit(str(error)) from None
