"""Check the committed anonymous inputs against the captured pixels, without either source PDF."""

import json
import unittest
from hashlib import sha256

from PIL import Image

from verify import CASES, HERE, UNIT, XS, has_ink


class ReferenceFixturesTest(unittest.TestCase):
    def test_manifests_match_every_visible_cell(self):
        for example, (_, digest, spec) in CASES.items():
            fixture = json.loads((HERE / f"example{example}.json").read_text("utf-8"))
            self.assertEqual(len(spec), len(fixture["pages"]))
            pixel_bits = bytearray()
            for i, page in enumerate(fixture["pages"]):
                with self.subTest(example=example, page=i + 1):
                    top = 30.29 if i == 0 else 15.25
                    dx, dy = round(XS[3] * 2) / 2, round((top + 255 * UNIT) * 2) / 2
                    rows = page["rows"]
                    self.assertEqual(len(rows), spec[i][1])
                    with Image.open(HERE / "grid" / f"example{example}-p{i + 1:02}.png") as image:
                        for row in range(12):
                            if row < len(rows):
                                item, plan, done = rows[row].split()
                                self.assertIn(item, fixture["medicines"])
                            else:
                                plan = done = "0" * 28
                            for role, mask in enumerate([plan, done]):
                                self.assertEqual(28, len(mask))
                                y = top + (320 + (2 * row + role) * 30) * UNIT - dy
                                actual = bytes(has_ink(image, XS[3 + s] - dx, XS[4 + s] - dx, y, y + 30 * UNIT)
                                               for s in range(28))
                                self.assertEqual(bytes(map(int, mask)), actual, f"pair {row + 1}, role {role + 1}")
                                pixel_bits.extend(actual)
                        for s, alias in enumerate(page["nurses"].split()):
                            for role, filled in enumerate([False, alias != "."]):
                                y = top + (1040 + role * 20) * UNIT - dy
                                self.assertEqual(filled, has_ink(image, XS[3 + s] - dx, XS[4 + s] - dx,
                                                                y, y + 20 * UNIT), f"signature slot {s + 1}")
            self.assertEqual(digest, sha256(pixel_bits).hexdigest())
