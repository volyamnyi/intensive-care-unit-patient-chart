import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs';
import { join, resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const specsDir = join(__dirname, 'specs');

function walk(dir) {
  const entries = readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const e of entries) {
    const p = join(dir, e.name);
    if (e.isDirectory()) files.push(...walk(p));
    else if (e.name.endsWith('.spec.ts') || e.name.endsWith('.spec.tsx')) files.push(p);
  }
  return files;
}

const files = walk(specsDir);
let changed = 0;

for (const fp of files) {
  let c = readFileSync(fp, 'utf8');
  const o = c;

  // Heal double prefixes introduced by earlier runs of this script.
  c = c.replaceAll('\\/icu\\/icu\\/', '\\/icu\\/');

  // Strip the stale /prescriptions/icu/ prefix BEFORE inserting /icu/ again.
  c = c.replace(/(\\\/prescriptions\\\/icu\\\/)/g, '\\/icu\\/');

  // Insert the /icu/ route prefix, but only when it is not already present.
  c = c.replace(/(?<!\\\/icu)\\\/doctor\\\/episode\\\//g, '\\/icu\\/doctor\\/episode\\/');
  c = c.replace(/(?<!\\\/icu)\\\/nurse\\\/episode\\\//g, '\\/icu\\/nurse\\/episode\\/');
  c = c.replaceAll("'/doctor/create-card'", "'/icu/doctor/create-card'");

  if (c !== o) {
    writeFileSync(fp, c, 'utf8');
    console.log(`  ${fp.replace(specsDir + '\\', '')}`);
    changed++;
  }
}
console.log(`Updated ${changed} files`);
