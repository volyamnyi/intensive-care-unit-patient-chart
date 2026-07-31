import { readFileSync, writeFileSync, readdirSync, statSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = join(__dirname, '..');

const includeDirs = [
  join(root, 'frontend', 'src'),
  join(root, 'tests', 'specs'),
  join(root, 'tests', 'pages'),
  join(root, 'tests', 'exploratory'),
];

const excludeDirs = ['node_modules'];

function walk(dir) {
  const entries = readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const e of entries) {
    const p = join(dir, e.name);
    if (e.isDirectory()) {
      if (!excludeDirs.includes(e.name)) files.push(...walk(p));
    } else if (e.name.endsWith('.ts') || e.name.endsWith('.tsx')) {
      files.push(p);
    }
  }
  return files;
}

const oldPrefix = '/prescriptions/icu';
const newPrefix = '/icu';
let changed = 0;

for (const dir of includeDirs) {
  for (const fp of walk(dir)) {
    let c = readFileSync(fp, 'utf8');
    const o = c;

    c = c.replaceAll(oldPrefix, newPrefix);

    if (c !== o) {
      writeFileSync(fp, c, 'utf8');
      const rel = fp.replace(root + '\\', '');
      console.log(`  ${rel}`);
      changed++;
    }
  }
}
console.log(`\nUpdated ${changed} files`);
