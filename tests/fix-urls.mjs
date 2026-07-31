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

  c = c.replaceAll('\\/doctor\\/episode\\/', '\\/prescriptions\\/icu\\/doctor\\/episode\\/');
  c = c.replaceAll('\\/nurse\\/episode\\/', '\\/prescriptions\\/icu\\/nurse\\/episode\\/');
  c = c.replaceAll("'/doctor/create-card'", "'/prescriptions/icu/doctor/create-card'");
  c = c.replace(/(\\\/prescriptions\\\/)(?!icu\\\/)nurse/g, '$1icu\\/nurse');

  if (c !== o) {
    writeFileSync(fp, c, 'utf8');
    console.log(`  ${fp.replace(specsDir + '\\', '')}`);
    changed++;
  }
}
console.log(`Updated ${changed} files`);
