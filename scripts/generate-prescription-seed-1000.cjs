const crypto = require('crypto');

function uuid(seed) {
  const h = crypto.createHash('sha256').update(String(seed)).digest('hex');
  return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20,32)}`;
}
function iid(p,m) { return uuid(`item-${p}-${m}`); }
function did(p,m,d) { return uuid(`day-${p}-${m}-${d}`); }
function pid2(p,m,d,pe) { return uuid(`part-${p}-${m}-${d}-${pe}`); }

const PERIODS = ['morning', 'day', 'evening', 'night'];

const surgeryMeds = [
  { n:'Цефтріаксон', m:'В/В', r:'1 г 1 р/д', d:'1 г', p:'morning' },
  { n:'Метронідазол', m:'В/В', r:'500 мг 3 р/д', d:'500 мг', p:'morning,day,evening' },
  { n:'NaCl 0.9%', m:'В/В', r:'800 мл/д', d:'800 мл', p:'morning' },
  { n:'Морфін', m:'В/В', r:'10 мг 4 р/д', d:'10 мг', p:'morning,day,evening,night' },
  { n:'Омепразол', m:'В/В', r:'40 мг 1 р/д', d:'40 мг', p:'morning' },
  { n:'Гепарин', m:'П/Ш', r:'5000 МО 2 р/д', d:'5000 МО', p:'morning,evening' },
  { n:'Дексаметазон', m:'В/В', r:'8 мг 1 р/д', d:'8 мг', p:'morning' },
  { n:'Пропофол', m:'В/В', r:'200 мг/год', d:'200 мг/год', p:'morning,day,evening,night' },
  { n:'Інсулін', m:'П/Ш', r:'за глікемією', d:'5 МО', p:'morning,day,evening,night' },
  { n:'Ондансетрон', m:'В/В', r:'8 мг 2 р/д', d:'8 мг', p:'morning,evening' },
];

const rehabMeds = [
  { n:'Пантопразол', m:'Перорально', r:'40 мг 1 р/д', d:'40 мг', p:'morning' },
  { n:'NaCl 0.9%', m:'В/В', r:'500 мл/д', d:'500 мл', p:'morning' },
  { n:'Дексаметазон', m:'Перорально', r:'4 мг 1 р/д', d:'4 мг', p:'morning' },
  { n:'Цефтріаксон', m:'В/В', r:'1 г 1 р/д', d:'1 г', p:'morning' },
  { n:'Омепразол', m:'Перорально', r:'20 мг 1 р/д', d:'20 мг', p:'morning' },
  { n:'Метронідазол', m:'Перорально', r:'500 мг 2 р/д', d:'500 мг', p:'morning,evening' },
  { n:'Глюкоза 5%', m:'В/В', r:'400 мл 2 р/д', d:'400 мл', p:'morning,evening' },
  { n:'Парацетамол', m:'Перорально', r:'500 мг 3 р/д', d:'500 мг', p:'morning,day,evening' },
  { n:'Мідозолам', m:'Перорально', r:'7.5 мг 1 р/д', d:'7.5 мг', p:'morning' },
  { n:'Ондансетрон', m:'Перорально', r:'8 мг 2 р/д', d:'8 мг', p:'morning,evening' },
];

// 21 days: 2026-07-22 to 2026-08-11
const dates = [];
const sd = new Date('2026-07-22');
for (let i = 0; i < 21; i++) {
  const d = new Date(sd); d.setDate(d.getDate() + i);
  dates.push(d.toISOString().slice(0, 10));
}

function lid(pid) {
  const n = pid - 1000;
  const ns = String(n).padStart(3, '0');
  const paddedPid = String(pid);
  return `bbbb${paddedPid}-${paddedPid}-${paddedPid}-${paddedPid}-000000000${ns}`;
}

const out = [];

for (let pid = 1001; pid <= 1050; pid++) {
  const isSrg = pid <= 1025;
  const deptId = isSrg ? 2 : 1;
  out.push(`INSERT INTO prescription_lists (id, patient_id, department_id, document_name, status, editing_user_id, editing_started_at, created_at, created_by, updated_at, updated_by, version, is_deleted) VALUES ('${lid(pid)}', ${pid}, ${deptId}, 'Листок лікарських призначень', 'Active', NULL, NULL, NOW(), 11, NOW(), 11, 0, FALSE) ON CONFLICT (id) DO NOTHING;`);
  const pool = isSrg ? surgeryMeds : rehabMeds;
  const cnt = 5;
  const used = new Set();
  const chosen = [];

  for (let mi = 0; mi < cnt; mi++) {
    let idx;
    do { idx = (pid * 13 + mi * 7) % pool.length; } while (used.has(idx));
    used.add(idx);
    chosen.push({ ...pool[idx], ti: mi });
  }

  for (const med of chosen) {
    const itemId = iid(pid, med.ti);
    const periods = med.p.split(',');

    out.push(`INSERT INTO prescription_items (id, list_id, medicine_name, medicine_method, regime, status, sort_order, created_at, created_by, updated_at, updated_by, version) VALUES ('${itemId}', '${lid(pid)}', '${med.n}', '${med.m}', '${med.r}', 'Active', ${med.ti}, NOW(), 11, NOW(), 11, 0) ON CONFLICT (id) DO NOTHING;`);

    const dayVals = [];
    for (let d = 0; d < 21; d++) {
      dayVals.push(`('${did(pid,med.ti,d)}', '${itemId}', '${dates[d]}', NOW(), 11, NOW(), 11, 0)`);
    }
    out.push(`INSERT INTO prescription_item_days (id, item_id, day_date, created_at, created_by, updated_at, updated_by, version) VALUES ${dayVals.join(', ')} ON CONFLICT DO NOTHING;`);

    for (let d = 0; d < 21; d++) {
      const parts = [];
      for (const per of PERIODS) {
        const active = periods.includes(per);
        const dose = active ? med.d : null;
        const ds = dose ? `'${dose}'` : 'NULL';

        let ip = active;
        let ic = false, icf = false;
        if (active) {
          if (d < 3) { ic = true; icf = true; }
          else if (d < 5) { ic = true; }
        }

        parts.push(`('${pid2(pid,med.ti,d,per)}', '${did(pid,med.ti,d)}', '${per}', ${ds}, ${ip}, FALSE, ${ic}, ${icf}, NOW(), 11, NOW(), 11, 0)`);
      }
      out.push(`INSERT INTO prescription_day_parts (id, day_id, period, dose, is_planned, is_planned_finished, is_completed, is_completed_finished, created_at, created_by, updated_at, updated_by, version) VALUES ${parts.join(', ')} ON CONFLICT DO NOTHING;`);
    }

    out.push('');
  }
}

console.log(out.join('\n'));
