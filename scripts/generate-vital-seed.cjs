const crypto = require('crypto');

function uuid(seed) {
  const h = crypto.createHash('sha256').update(String(seed)).digest('hex');
  return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20,32)}`;
}

function plid(pid) {
  if (pid >= 2001) {
    const n = pid - 2000;
    const ns = String(n).padStart(3, '0');
    return `cccc${pid}-${pid}-${pid}-${pid}-000000000${ns}`;
  }
  const n = pid - 1000;
  const ns = String(n).padStart(3, '0');
  return `bbbb${pid}-${pid}-${pid}-${pid}-000000000${ns}`;
}

function vslid(pid) { return uuid(`vsl-${pid}`); }
function vsdid(lid, date) { return uuid(`vsd-${lid}-${date}`); }
function vseid(did, period) { return uuid(`vse-${did}-${period}`); }

const PERIODS = ['morning', 'day', 'evening', 'night'];

const dates = [];
const sd = new Date('2026-07-22');
for (let i = 0; i < 21; i++) {
  const d = new Date(sd);
  d.setDate(d.getDate() + i);
  dates.push(d.toISOString().slice(0, 10));
}

// Vital sign value generators with some realistic variation
function seededRandom(seed) {
  const h = crypto.createHash('sha256').update(String(seed)).digest('hex');
  return parseInt(h.slice(0, 8), 16) / 0xffffffff;
}

function temp(pid, d, per) {
  // 36.5-37.2 normal, slight diurnal variation
  const r = seededRandom(`temp-${pid}-${d}-${per}`);
  return (36.5 + r * 0.7).toFixed(1);
}
function sys(pid, d, per) {
  const r = seededRandom(`sys-${pid}-${d}-${per}`);
  return Math.round(110 + r * 30);
}
function dia(pid, d, per) {
  const r = seededRandom(`dia-${pid}-${d}-${per}`);
  return Math.round(70 + r * 20);
}
function spo2val(pid, d, per) {
  const r = seededRandom(`spo2-${pid}-${d}-${per}`);
  return Math.round(94 + r * 5);
}
function pulse(pid, d, per) {
  const r = seededRandom(`pulse-${pid}-${d}-${per}`);
  return Math.round(70 + r * 30);
}
function stoolVal(pid, d, per) {
  // Most entries null, occasional 'норма', very rare 'рідкий'
  const r = seededRandom(`stool-${pid}-${d}-${per}`);
  if (r < 0.7) return 'NULL';
  if (r < 0.9) return "'норма'";
  return "'рідкий'";
}
function pain(pid, d, per) {
  const r = seededRandom(`pain-${pid}-${d}-${per}`);
  if (r < 0.3) return '0';
  if (r < 0.7) return '1';
  if (r < 0.9) return '2';
  return '3';
}

const listInserts = [];
const dayInserts = [];
const entryInserts = [];

// 90 vital_sign_lists + days + entries
const allPids = [];
for (let pid = 1001; pid <= 1050; pid++) allPids.push(pid);
for (let pid = 2001; pid <= 2040; pid++) allPids.push(pid);

for (const pid of allPids) {
  const listId = plid(pid);
  const vslId = vslid(pid);

  // vital_sign_lists row (phase 1)
  listInserts.push(`INSERT INTO vital_sign_lists (id, prescription_list_id, created_at, created_by, updated_at, updated_by, version, is_deleted) VALUES ('${vslId}', '${listId}', NOW(), 11, NOW(), 11, 0, FALSE) ON CONFLICT (id) DO NOTHING;`);

  // vital_sign_days (phase 2)
  const dayRows = [];
  // vital_sign_entries (phase 3) — collect per-list
  const listEntryRows = [];

  for (let d = 0; d < 21; d++) {
    const date = dates[d];
    const dayId = vsdid(vslId, date);
    dayRows.push(`('${dayId}', '${vslId}', '${date}', NOW(), 11, NOW(), 11, 0)`);

    // 4 entries per day
    const entryRows = [];
    for (const per of PERIODS) {
      const eid = vseid(dayId, per);
      const isFilled = d < 3;
      if (isFilled) {
        const t = temp(pid, d, per);
        const s = sys(pid, d, per);
        const di = dia(pid, d, per);
        const sp = spo2val(pid, d, per);
        const pu = pulse(pid, d, per);
        const st = stoolVal(pid, d, per);
        const pa = pain(pid, d, per);
        entryRows.push(`('${eid}', '${dayId}', '${per}', ${t}, ${s}, ${di}, ${sp}, ${pu}, ${st}, ${pa}, NOW(), 11, NOW(), 11, 0)`);
      } else {
        entryRows.push(`('${eid}', '${dayId}', '${per}', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW(), 11, NOW(), 11, 0)`);
      }
    }
    listEntryRows.push(`INSERT INTO vital_sign_entries (id, day_id, period, temperature, systolic_bp, diastolic_bp, spo2, pulse, stool, pain_score, created_at, created_by, updated_at, updated_by, version) VALUES ${entryRows.join(', ')} ON CONFLICT DO NOTHING;`);
  }
  dayInserts.push(`INSERT INTO vital_sign_days (id, vital_list_id, day_date, created_at, created_by, updated_at, updated_by, version) VALUES ${dayRows.join(', ')} ON CONFLICT DO NOTHING;`);
  entryInserts.push(...listEntryRows);
}

// Phase 1: lists
const out = [...listInserts, '', '-- vital_sign_days', ...dayInserts, '', '-- vital_sign_entries', ...entryInserts];

console.log(out.join('\n'));
