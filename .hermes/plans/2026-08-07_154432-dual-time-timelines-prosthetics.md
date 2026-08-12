# «2 лінії часу» в обліку часу процесу виробництва протезів — план реалізації

> **Для Hermes:** реалізовувати за допомогою skill `subagent-driven-development`, по задачі за раз, із two-stage review (відповідність спеці, потім якість коду).

**Goal:** Запровадити в модулі «Виробництво протезів» облік часу у вигляді двох ліній: **загальний час процесу** (тік-так настінного годинника, триває навіть під час паузи) та **активний (робочий) час** (зупиняється на паузі), — з відображенням у всьому UI та PDF-звіті.

**Architecture:** Поточні лічильники (`totalActiveSeconds`/`totalIdleSeconds`) — персистентні накопичувачі, які оновлюються на завершенні кроку/ресьюмі. Нові «живі» значення двох ліній обчислюються **на льоту** в `FlowInstanceService.toResponse()` з існуючих timestamp-ів (`startTime`/`endTime`/`pausedAt`/`resumedAt`) + накопичувачів — **без міграції БД**. UI «тікає» між запитами локально (setInterval), як уже робить таймер кроку в `WizardScreen`.

**Tech Stack:** Java 25 / Spring Boot 4.1 (модуль `prosthesis-manufacturing`), React 19 + TS 6 (сторінки `prosthetics/*`), Vitest, Playwright E2E, Liquibase (міграція НЕ потрібна у варіанті A).

---

## 1. Поточний стан (розвідка)

| Що | Де | Стан |
|---|---|---|
| `totalActiveSeconds` (накопичений активний час) | `FlowInstance` + збільшується в `completeStep()` | ✅ є |
| `totalIdleSeconds` (накопичені паузи) | `FlowInstance` + збільшується в `resume()` | ✅ є |
| `startTime` / `endTime` / `pausedAt` / `resumedAt` / `pauseCategory` | `FlowInstance` (міграція `011-prosthesis-pause-tracking.sql`) | ✅ є |
| Живий таймер кроку | `WizardScreen.tsx:248-254,529` — `seconds` + `setInterval`, скидається на кожному кроці | ✅ є (кроковий) |
| Відображення часу | `DoneScreen` («Активний час»), `FailedScreen` («Активний час» + «Час простою»), PDF (`ProstheticsPdfService:96-99` — «Активний час» + «Простої»), `DashboardPage` — колонки часу НЕМАЄ | частково |
| **Загальний час (стінний)** | **НЕ зберігається і НЕ віддається** — виводиться тільки через `endTime - startTime` на фронтенді ніде | ❌ немає |

**Діри в обліку (впливають на точність «активної» лінії):**
1. `backward()` (рядки 274–281): скасоване виконання кроку НЕ додає свій час у `totalActiveSeconds` — активний час недозараховується при поверненні на крок назад.
2. `fail()` (450–465) та `markQcFailed()` (467–473): поточний IN_PROGRESS крок не зараховується перед `endTime` — останній сегмент роботи втрачається (для `COMPLETED` шлях коректний — `completeStep()` зараховує до `advance()`).
3. Стан `WAITING_REVIEW` (очікування рішення gate) — не пауза і не робота: у `totalSeconds` входить, у `active/idle` — ні (за дизайном; розбіжність `total − active − idle` = «очікування»).

---

## 2. Інтерпретація вимоги

> «Облік часу: Повинно бути "2 лінії часу". При паузі процесу, загальний час продовжує нараховуватись, але разом з тим друга по поточному процесу зупиняється.»

| Лінія | Що рахує | Під час паузи |
|---|---|---|
| **Лінія 1 — «Загальний час»** (wall-clock) | `endTime − startTime` (або `now − startTime` для активного) | **триває** |
| **Лінія 2 — «Активний час»** (робочий) | `totalActiveSeconds` + поточний сегмент (`now − resumedAt` при IN_PROGRESS) | **зупиняється** |

Третя похідна — «Паузи» (`totalIdleSeconds` + поточний сегмент `now − pausedAt` при PAUSED) — вже відображається у `FailedScreen`/PDF; залишаємо як допоміжну.

---

## 3. Варіанти реалізації (порівняння)

### Варіант A — похідні значення в DTO (РЕКОМЕНДОВАНИЙ)
Нові поля в `FlowInstanceResponse`: `totalSeconds`, `activeSeconds`, `idleSeconds` — обчислюються в `toResponse()` через чисту функцію `TimeStats.of(instance, now)`.

- ✅ **Без міграції БД** — працює для вже існуючих екземплярів (все виводиться з наявних timestamp-ів).
- ✅ Один канонічний джерело істини (сервер); PDF, UI, API — все від нього.
- ✅ Live-точність: «тікання» між запитами робить фронтенд (як уже зроблено для таймера кроку).
- ✅ Чиста функція → тривіальні unit-тести (без Spring, без Clock).
- ⚠️ Значення «старіють» між запитами → UI тікає локально (покрито).
- ⚠️ `LocalDateTime.now()` у сервісі — тести з допуском; за потреби — бін `Clock` (YAGNI, поки не потрібно).

### Варіант B — персистентний лічильник `total_elapsed_seconds` (міграція 013)
Нова колонка, оновлюється на pause/resume/complete.

- ✅ «Матеріалізоване» значення в БД.
- ❌ Надлишкове: `endTime − startTime` дає те саме; ризик дрейфу при ручних правках timestamp; міграція для 3-х рядків коду.
- ❌ Для live-відображення все одно потрібен дельта-розрахунок.

### Варіант C — подієва таблиця часу `prosthetics_time_events` (start/pause/resume/complete)
Повна event-sourced історія: кожна пауза = запис (категорія, початок, кінець, тривалість).

- ✅ Найкраще для аудиту/аналітики: історія пауз, розбивка по станах, реконструкція будь-якого моменту.
- ❌ Найважчий: нова таблиця, запис подій у всіх переходах станів, міграція, міграція даних для існуючих екземплярів.
- ➡️ Окремий епік, якщо з'явиться вимога «історія пауз» / «аналітика часу по категоріях». Для поточної вимоги — over-engineering.

### Варіант D — тільки фронтенд (обчислення в UI)
- ✅ Найшвидший.
- ❌ Неканонічний: PDF/API не отримують значення; дублювання логіки в 5 компонентах; вимога про «облік часу» читається як серверна.

**Рекомендація: Варіант A** (+ закриття дір обліку з розділу 1.1–1.2). Варіант C — як опція розширення, якщо потрібна історія пауз.

---

## 4. Детальний план (Варіант A)

### Task 1: Чиста функція `TimeStats` + unit-тести

**Objective:** Один обчислювач двох ліній + пауз, тестований без Spring.

**Files:**
- Create: `backend/prosthesis-manufacturing/src/main/java/com/superhumans/prosthesismanufacturing/service/TimeStats.java`
- Create: `backend/prosthesis-manufacturing/src/test/java/com/superhumans/prosthesismanufacturing/service/TimeStatsTest.java`

**Step 1: Тест (TDD)**
```java
package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class TimeStatsTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 8, 7, 12, 0, 0);

    private FlowInstance instance() {
        FlowInstance i = FlowInstance.builder()
                .status(FlowInstanceStatus.IN_PROGRESS)
                .startTime(now.minusHours(2))          // 2 год тому
                .resumedAt(now.minusMinutes(30))       // працюємо останні 30 хв
                .totalActiveSeconds(3600L)             // 1 год накопичено
                .totalIdleSeconds(300L)                // 5 хв пауз
                .build();
        return i;
    }

    @Test
    void totalKeepsAccruingWhileActiveSegmentIsLive() {
        TimeStats s = TimeStats.of(instance(), now);
        assertThat(s.totalSeconds()).isEqualTo(7200);      // 2 год — вся стіна
        assertThat(s.activeSeconds()).isEqualTo(5400);     // 3600 + 1800 (поточний сегмент)
        assertThat(s.idleSeconds()).isEqualTo(300);
    }

    @Test
    void activeStopsButTotalContinuesWhilePaused() {
        FlowInstance i = instance();
        i.setStatus(FlowInstanceStatus.PAUSED);
        i.setPausedAt(now.minusMinutes(10));
        i.setResumedAt(null); // останній робочий сегмент закінчився на паузі
        TimeStats s = TimeStats.of(i, now);
        assertThat(s.totalSeconds()).isEqualTo(7200);      // стіна триває
        assertThat(s.activeSeconds()).isEqualTo(3600);     // заморожено
        assertThat(s.idleSeconds()).isEqualTo(900);        // 300 + 600 поточної паузи
    }

    @Test
    void newInstanceHasZeroTotal() {
        FlowInstance i = FlowInstance.builder().status(FlowInstanceStatus.NEW).build();
        TimeStats s = TimeStats.of(i, now);
        assertThat(s.totalSeconds()).isZero();
        assertThat(s.activeSeconds()).isZero();
        assertThat(s.idleSeconds()).isZero();
    }

    @Test
    void terminalInstanceIsFrozenAtEndTime() {
        FlowInstance i = instance();
        i.setStatus(FlowInstanceStatus.COMPLETED);
        i.setEndTime(now.minusMinutes(5)); // завершився 5 хв тому
        TimeStats s = TimeStats.of(i, now);
        assertThat(s.totalSeconds()).isEqualTo(6900);      // endTime - startTime = 1:55:00
        assertThat(s.activeSeconds()).isEqualTo(3600);     // без live-сегмента
    }
}
```

**Step 2: Перевірити, що падає** — Run: `mvn test -pl prosthesis-manufacturing -Dtest=TimeStatsTest -DfailIfNoTests=false` (в `backend/`), expected: FAIL (клас не існує). *(Дозволено лише як локальну діагностику — фінальна верифікація через CI.)*

**Step 3: Реалізація**
```java
package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/** Живі значення двох ліній часу процесу: загальний (стінний) та активний (робочий). */
public record TimeStats(long totalSeconds, long activeSeconds, long idleSeconds) {

    public static TimeStats of(FlowInstance instance, LocalDateTime now) {
        long total = 0;
        if (instance.getStartTime() != null) {
            LocalDateTime end = instance.getEndTime() != null ? instance.getEndTime() : now;
            total = Math.max(0, Duration.between(instance.getStartTime(), end).getSeconds());
        }
        long active = instance.getTotalActiveSeconds() == null ? 0 : instance.getTotalActiveSeconds();
        if (instance.getStatus() == FlowInstanceStatus.IN_PROGRESS && instance.getResumedAt() != null) {
            active += Math.max(0, Duration.between(instance.getResumedAt(), now).getSeconds());
        }
        long idle = instance.getTotalIdleSeconds() == null ? 0 : instance.getTotalIdleSeconds();
        if (instance.getStatus() == FlowInstanceStatus.PAUSED && instance.getPausedAt() != null) {
            idle += Math.max(0, Duration.between(instance.getPausedAt(), now).getSeconds());
        }
        return new TimeStats(total, active, idle);
    }
}
```

**Step 4: Перевірити pass** — той самий mvn-запуск, expected: 4 passed.

**Step 5: Commit**
```bash
git add backend/prosthesis-manufacturing/src/main/java/com/superhumans/prosthesismanufacturing/service/TimeStats.java \
        backend/prosthesis-manufacturing/src/test/java/com/superhumans/prosthesismanufacturing/service/TimeStatsTest.java
git commit -m "feat: TimeStats — live total/active/idle time computation for flow instances"
```

---

### Task 2: DTO + `toResponse()` віддає дві лінії

**Objective:** Клієнти отримують `totalSeconds` / `activeSeconds` / `idleSeconds` у кожному `FlowInstanceResponse`.

**Files:**
- Modify: `backend/prosthesis-manufacturing/src/main/java/com/superhumans/prosthesismanufacturing/dto/FlowInstanceResponse.java` — додати 3 поля (`Long totalSeconds; Long activeSeconds; Long idleSeconds;`) після `totalIdleSeconds` (рядок 28).
- Modify: `backend/prosthesis-manufacturing/src/main/java/com/superhumans/prosthesismanufacturing/service/FlowInstanceService.java:107-140` — на початку `toResponse()`:
```java
TimeStats stats = TimeStats.of(instance, LocalDateTime.now());
response.setTotalSeconds(stats.totalSeconds());
response.setActiveSeconds(stats.activeSeconds());
response.setIdleSeconds(stats.idleSeconds());
```
- Modify (тест): `backend/prosthesis-manufacturing/src/test/java/com/superhumans/prosthesismanufacturing/mapper/InstanceMapperTest.java` — перевірити, що нові поля не затираються маппером (MapStruct ігнорує їх — вони виставляються вручну; достатньо assertion у FlowInstanceServiceTest нижче).

**Step 1: Тест у `FlowInstanceServiceTest`** — новий тест `toResponseExposesLiveTimeStats` (mock `instanceRepository.findById` повертає екземпляр IN_PROGRESS з `startTime = now-2h`, `resumedAt = now-30m`, `totalActiveSeconds = 3600`): очікуємо `response.getTotalSeconds() == 7200`, `response.getActiveSeconds() == 5400`, `response.getIdleSeconds() >= 0`.

**Step 2–4:** запустити тест → FAIL (полів немає) → реалізація → PASS.

**Step 5: Commit**
```bash
git commit -m "feat: expose live total/active/idle seconds in FlowInstanceResponse"
```

---

### Task 3: Закрити діри обліку (backward / fail / markQcFailed)

**Objective:** Жоден відпрацьований сегмент не губиться: скасований крок і крок на момент провалу зараховуються в `totalActiveSeconds`.

**Files:**
- Modify: `FlowInstanceService.java`
  - `backward()` (274–281): перед `setStatus(CANCELLED)`:
    ```java
    long delta = secondsSince(e.getStartedAt(), instance.getResumedAt(), now);
    e.setActiveSeconds((e.getActiveSeconds() == null ? 0L : e.getActiveSeconds()) + delta);
    e.setStatus(StepExecutionStatus.CANCELLED);
    e.setCompletedAt(now);
    executionRepository.save(e);
    instance.setTotalActiveSeconds(instance.getTotalActiveSeconds() + delta);
    ```
  - `fail()` (450–465) та `markQcFailed()` (467–473): перед `setEndTime` — знайти поточний IN_PROGRESS execution і зарахувати `secondsSince(startedAt, resumedAt, now)` в `execution.activeSeconds` + `instance.totalActiveSeconds` (винести в приватний хелпер `accrueCurrentExecution(instance, now)`).
- Modify (тест): `FlowInstanceServiceTest.java` — `backwardAccruesCancelledExecutionTime`, `failAccruesCurrentExecutionTime`.

**Step 5: Commit**
```bash
git commit -m "fix: accrue cancelled and failing step time into active timeline"
```

---

### Task 4: Типи фронтенду + хелпер форматування

**Objective:** TS-тип `FlowInstance` і спільний форматер «Год:Хв:Сек».

**Files:**
- Modify: `frontend/src/prosthetics/types.ts` — у `FlowInstance` (після рядка 124):
  ```ts
  totalSeconds: number | null;
  activeSeconds: number | null;
  idleSeconds: number | null;
  ```
- Create: `frontend/src/prosthetics/formatTime.ts`:
  ```ts
  export function formatTime(seconds: number | null | undefined): string {
    const s = Math.max(0, Math.floor(seconds ?? 0));
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return h > 0
      ? `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
      : `${m}:${String(sec).padStart(2, '0')}`;
  }
  ```
- Vitest: `frontend/src/test/prosthetics/formatTime.test.ts` (edge: 0 → «0:00», 3661 → «1:01:01», null → «0:00»).

**Commit:** `feat: formatTime helper and FlowInstance time fields`

---

### Task 5: WizardScreen — «2 лінії часу» в шапці

**Objective:** Замість таймера кроку — два лічильники: «Загальний час» (тікає завжди до термінального стану) та «Активний час» (тікає лише при IN_PROGRESS).

**Files:** Modify: `frontend/src/pages/prosthetics/process/WizardScreen.tsx`

**Step 1: Тест** `frontend/src/test/prosthetics/WizardScreen.test.tsx` (fake timers, `vi.useFakeTimers()`):
- Рендер IN_PROGRESS: обидва лічильники збільшуються після `act(() => vi.advanceTimersByTime(2000))`.
- Рендер PAUSED: «Загальний час» збільшується, «Активний час» заморожений.

**Step 2:** перевірити FAIL → **Step 3:** реалізація:
- Прибрати кроковий `seconds`/`fmt` (248–254, 529) або залишити як «Час на кроці» в тілі (див. Відкриті питання).
- У шапці (518–531) два бейджі:
  ```tsx
  const terminal = instance.status === 'COMPLETED' || instance.status === 'FAILED' || instance.status === 'FAILED_QC';
  const [tick, setTick] = useState(0);
  useEffect(() => {
    if (terminal) return;
    const t = setInterval(() => setTick((s) => s + 1), 1000);
    return () => clearInterval(t);
  }, [terminal]);
  const activeTicking = instance.status === 'IN_PROGRESS';
  const totalShown = (instance.totalSeconds ?? 0) + (terminal ? 0 : tick);
  const activeShown = (instance.activeSeconds ?? 0) + (activeTicking ? tick : 0);
  // <Timer/> Загальний час: {formatTime(totalShown)} · Активний час: {formatTime(activeShown)}
  ```
- Логіку «тікання» винести в хук `useDualTimeline(instance)` у `frontend/src/prosthetics/useDualTimeline.ts` (перевикористання на Dashboard за потреби; DRY).

**Step 4:** PASS → **Commit:** `feat: dual time lines (total keeps running, active stops on pause) in wizard`

---

### Task 6: DashboardPage — колонка часу

**Objective:** У таблиці процесів видно обидві лінії.

**Files:** Modify: `frontend/src/pages/prosthetics/DashboardPage.tsx` — після колонки «Статус» (205–207) додати `TableHead>Час (заг./акт.)</TableHead>`; у рядку:
```tsx
<TableCell className="font-mono text-xs">
  {formatTime(instance.totalSeconds)} / {formatTime(instance.activeSeconds)}
</TableCell>
```
**Test:** `DashboardPage.test.tsx` — колонка рендериться з `2:00:00 / 1:00:00` для фікстури з `totalSeconds: 7200, activeSeconds: 3600`.

**Commit:** `feat: show total/active time column on prosthetics dashboard`

---

### Task 7: DoneScreen / FailedScreen — «Загальний час»

**Objective:** Підсумкові екрани показують усі три значення.

**Files:** Modify:
- `frontend/src/pages/prosthetics/process/DoneScreen.tsx:151-155` — grid 3 → 4: `<Stat label="Загальний час" value={formatHours(instance.totalSeconds)} />` + існуючий «Активний час».
- `frontend/src/pages/prosthetics/process/FailedScreen.tsx:296-303` — додати рядок «Загальний час» перед «Активний час».
- (За бажанням) `ProcessOverview.tsx` — якщо показує час, додати ті ж дві лінії.

**Tests:** оновити `DoneScreen.test.tsx` (фікстура + новий Stat), `FailedScreen.test.tsx`.

**Commit:** `feat: show total process time on done/failed screens`

---

### Task 8: PDF-звіт — «Загальний час»

**Objective:** Звіт містить усі три рядки часу.

**Files:** Modify: `backend/prosthesis-manufacturing/src/main/java/com/superhumans/prosthesismanufacturing/service/ProstheticsPdfService.java:94-99` — після «Завершення» додати:
```java
if (instance.getStartTime() != null && instance.getEndTime() != null) {
    infoLine(doc, font, "Загальний час",
            formatDuration(Math.max(0, Duration.between(instance.getStartTime(), instance.getEndTime()).getSeconds())));
}
```
(для термінальних екземплярів `endTime` завжди виставлено; `formatDuration` уже існує).

**Test:** юніт-тест PDF — якщо для `ProstheticsPdfService` є тест-харнесс (перевірити наявність; інакше достатньо E2E нижче).

**Commit:** `feat: total time line in prosthetics PDF report`

---

### Task 9: E2E — prosthetics-workflow.spec.ts

**Objective:** E2E-підтвердження поведінки «пауза зупиняє активну лінію, але не загальну».

**Files:** Modify: `tests/specs/prosthetics/prosthetics-workflow.spec.ts`

**Кроки (у наявному сценарії):**
1. Після старту процесу прочитати текст обох бейджів у шапці wizard (локатори за текстом «Загальний час» / «Активний час»).
2. Натиснути «Пауза» → вибрати категорію → підтвердити.
3. `page.waitForTimeout(2500)` (або `expect.poll` на текст) → «Загальний час» зріс, «Активний час» не змінився (порівняти з попереднім значенням).
4. «Відновити» → продовжити сценарій.
- **Антифлейк:** порівнювати значення, а не абсолют; `expect.poll(...)` з `timeout: 5000`; не покладатись на секундну точність (допуск ±2 с); CI retries=2 вже налаштовано.

**Commit:** `test(e2e): pause freezes the active timeline while total keeps running`

---

### Task 10: Pre-flight + CI

**Step 1 (локально, дозволено):**
```bash
cd backend && mvn compile -q                      # компіляція
cd frontend && npm run lint && npx tsc --noEmit && npm run build
```
**Step 2:** `git push origin main` → `.github/workflows/playwright.yml` запустить усі 6 джобів (format-check, backend-test, backend-integration, frontend-test, e2e-test, build). Локальні `mvn test`/`npm test`/Playwright — ЗАБОРОНЕНО (AGENTS.md); локально дозволено лише одиничні діагностичні запуски конкретного тесту (Tasks 1–5) для TDD-циклу.

**Step 3:** `gh run watch <run-id>` → поллінг; при падіннях — `gh run view --job <id> --log`, виправлення новим комітом (без amend/force-push).

---

## 5. Зведений список файлів

| Файл | Зміна |
|---|---|
| `backend/prosthesis-manufacturing/.../service/TimeStats.java` | **Create** — чистий обчислювач |
| `backend/prosthesis-manufacturing/.../service/TimeStatsTest.java` | **Create** — unit-тести |
| `backend/prosthesis-manufacturing/.../dto/FlowInstanceResponse.java` | +3 поля |
| `backend/prosthesis-manufacturing/.../service/FlowInstanceService.java` | toResponse + accrue у backward/fail/markQcFailed |
| `backend/prosthesis-manufacturing/.../service/FlowInstanceServiceTest.java` | +3 тести |
| `backend/prosthesis-manufacturing/.../service/ProstheticsPdfService.java` | +«Загальний час» |
| `frontend/src/prosthetics/types.ts` | +3 поля типу |
| `frontend/src/prosthetics/formatTime.ts` | **Create** — форматер |
| `frontend/src/prosthetics/useDualTimeline.ts` | **Create** — хук тікання (DRY) |
| `frontend/src/pages/prosthetics/process/WizardScreen.tsx` | 2 бейджі замість таймера кроку |
| `frontend/src/pages/prosthetics/DashboardPage.tsx` | колонка «Час (заг./акт.)» |
| `frontend/src/pages/prosthetics/process/DoneScreen.tsx` | Stat «Загальний час» |
| `frontend/src/pages/prosthetics/process/FailedScreen.tsx` | рядок «Загальний час» |
| `frontend/src/test/prosthetics/*` | +unit-тести (formatTime, WizardScreen, Dashboard, Done, Failed) |
| `tests/specs/prosthetics/prosthetics-workflow.spec.ts` | +пауза-перевірка |

**Міграція БД: НЕ потрібна** (Варіант A) — наступний вільний номер changeset 013 залишається незайнятим.

---

## 6. Ризики / відкриті питання

1. **WAITING_REVIEW (очікування gate):** час іде в «Загальний час», але не в «Активний» і не в «Паузи». Чи влаштовує, чи вважати це «простоєм» (тоді додати в idle-гілку `TimeStats`)? ⚠️ **Потребує підтвердження користувача.**
2. **Таймер кроку в Wizard:** прибрати повністю (чисті «2 лінії» — за замовчуванням) чи залишити «Час на кроці» додатково? За замовчуванням — прибрати з шапки, оскільки вимога фіксує саме 2 лінії.
3. **Точність секунд:** `Duration.getSeconds()` округлює вниз; накопичені дробові секунди при багатьох паузах можуть дати розбіжність ±1 с між `total` і `active+idle` — прийнятно, документується.
4. **Clock-абстракція:** `LocalDateTime.now()` в сервісі ускладнює точні тести часу — якщо в майбутньому знадобиться сувора детермінованість, завести бін `Clock` (зараз YAGNI).
5. **BLOCKED_PATIENT / BLOCKED_MATERIAL:** статуси існують в enum, але сервіс їх не використовує; якщо з'являться — вирішити, чи рахувати як паузу (idle). Зараз не чіпаємо.
6. **E2E-таймінг:** перевірка «загальний час зріс / активний заморожений» — найризикованіше місце для флейків у CI (ранкові прогони). Мінімізація: `expect.poll` + допуски; за потреби — перевіряти тільки візуальну наявність двох ліній, а сувору семантику покрити unit-тестами `TimeStatsTest` та Vitest.
