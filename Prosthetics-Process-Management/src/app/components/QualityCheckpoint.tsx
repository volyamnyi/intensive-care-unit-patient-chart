import { useState } from "react";
import { CheckCircle, XCircle, Paperclip } from "lucide-react";
import { Process, ProcessTemplate } from "./mockData";

interface Props {
  process: Process;
  template: ProcessTemplate;
  stageIndex: number;
  onPass: () => void;
  onRework: () => void;
  onFail: () => void;
}

export function QualityCheckpoint({ process, template, stageIndex, onPass, onRework, onFail }: Props) {
  const [showDecision, setShowDecision] = useState(false);
  const [checklist, setChecklist] = useState([false, false, false, false]);
  const [measurement, setMeasurement] = useState("");
  const [notes, setNotes] = useState("");

  const stage = template.stages[stageIndex];
  const stageName = stage?.name || "Контроль якості";

  const criteria = [
    "Розміри відповідають технічним вимогам (допуск ±1 мм)",
    "Поверхня без видимих дефектів і тріщин",
    "Матеріал однорідний, без повітряних включень",
    "Механічна міцність відповідає стандарту",
  ];

  const allChecked = checklist.every(Boolean);

  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-3xl mx-auto">
        <div className="bg-gray-800 text-white rounded px-5 py-3 mb-5 flex items-center gap-3">
          <span className="text-xl">◆</span>
          <div>
            <div className="text-xs text-gray-400 mb-0.5">Контрольна точка якості</div>
            <h2 className="text-white">{stageName}</h2>
          </div>
          <div className="ml-auto text-xs text-gray-400">
            {process.patientName} — {process.orderNumber}
          </div>
        </div>

        <div className="grid grid-cols-[1fr_280px] gap-4">
          <div className="space-y-4">
            {/* Checkpoint info */}
            <div className="bg-white border border-gray-200 rounded p-5">
              <h3 className="text-gray-700 text-sm mb-2">Опис контрольної точки</h3>
              <p className="text-gray-600 text-sm leading-relaxed">
                Перевірка якості виробу після завершення виробничого етапу. Необхідно виміряти всі критичні розміри та перевірити відповідність технічним вимогам перед переходом до наступного етапу.
              </p>
            </div>

            {/* Acceptance criteria checklist */}
            <div className="bg-white border border-gray-200 rounded p-5">
              <h3 className="text-gray-700 text-sm mb-4">Критерії прийому</h3>
              <div className="space-y-3">
                {criteria.map((c, i) => (
                  <label key={i} className="flex items-start gap-3 cursor-pointer hover:bg-gray-50 p-2 rounded -mx-2">
                    <input
                      type="checkbox"
                      checked={checklist[i]}
                      onChange={() => { const upd = [...checklist]; upd[i] = !upd[i]; setChecklist(upd); }}
                      className="mt-0.5 rounded border-gray-400 w-4 h-4"
                    />
                    <span className={`text-sm ${checklist[i] ? "text-gray-500 line-through" : "text-gray-700"}`}>{c}</span>
                  </label>
                ))}
              </div>
              <div className="mt-3 pt-3 border-t border-gray-100 text-xs text-gray-500">
                {checklist.filter(Boolean).length} з {criteria.length} критеріїв виконано
              </div>
            </div>

            {/* Measurements */}
            <div className="bg-white border border-gray-200 rounded p-5">
              <h3 className="text-gray-700 text-sm mb-4">Виміри</h3>
              <div className="space-y-3">
                {[
                  { label: "Довжина (мм)", planned: "320", unit: "мм" },
                  { label: "Ширина проксимальна (мм)", planned: "95", unit: "мм" },
                  { label: "Товщина стінки (мм)", planned: "4-6", unit: "мм" },
                ].map((m, i) => (
                  <div key={i} className="grid grid-cols-3 gap-3 items-center text-sm">
                    <span className="text-gray-600">{m.label}</span>
                    <div className="text-center">
                      <span className="text-xs text-gray-400 block">План</span>
                      <span className="text-gray-700">{m.planned} {m.unit}</span>
                    </div>
                    <div>
                      <span className="text-xs text-gray-400 block">Факт</span>
                      <input type="number" className="w-full border border-gray-300 rounded px-2 py-1 text-sm" placeholder="0" />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Notes and attachments */}
            <div className="bg-white border border-gray-200 rounded p-5">
              <h3 className="text-gray-700 text-sm mb-3">Нотатки та вкладення</h3>
              <textarea
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm h-20 resize-none focus:outline-none focus:border-gray-500"
                placeholder="Додаткові спостереження або коментарі..."
                value={notes}
                onChange={e => setNotes(e.target.value)}
              />
              <div className="mt-3 border-2 border-dashed border-gray-300 rounded p-3 text-center">
                <div className="flex items-center justify-center gap-2 text-gray-400 text-sm">
                  <Paperclip size={14} />
                  <span>Прикріпити фото або документи</span>
                </div>
              </div>
            </div>
          </div>

          {/* Decision panel */}
          <div className="space-y-3">
            <div className="bg-white border border-gray-200 rounded p-4">
              <h3 className="text-gray-700 text-sm mb-3">Рішення</h3>
              <div className="space-y-2">
                <button
                  onClick={onPass}
                  disabled={!allChecked}
                  className="w-full flex items-center gap-2 bg-gray-800 text-white px-4 py-3 rounded hover:bg-gray-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                  <CheckCircle size={18} />
                  <div className="text-left">
                    <div className="text-sm">Пройдено ✓</div>
                    <div className="text-xs text-gray-300">Перейти до наступного етапу</div>
                  </div>
                </button>
                <button
                  onClick={() => setShowDecision(true)}
                  className="w-full flex items-center gap-2 border border-gray-300 text-gray-700 px-4 py-3 rounded hover:bg-gray-50 transition-colors"
                >
                  <XCircle size={18} />
                  <div className="text-left">
                    <div className="text-sm">Не пройдено ✗</div>
                    <div className="text-xs text-gray-500">Перевірка не виконана</div>
                  </div>
                </button>
              </div>
              {!allChecked && (
                <p className="text-xs text-gray-400 mt-2">Позначте всі критерії для проходження</p>
              )}
            </div>

            <div className="bg-gray-50 border border-gray-200 rounded p-4 text-xs text-gray-500 space-y-1">
              <div><strong>Процес:</strong> {template.name}</div>
              <div><strong>Пацієнт:</strong> {process.patientName}</div>
              <div><strong>Замовлення:</strong> {process.orderNumber}</div>
              <div><strong>Етап:</strong> {stageName}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Failure decision dialog */}
      {showDecision && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded border border-gray-300 p-6 max-w-sm w-full mx-4 shadow-xl">
            <h3 className="text-gray-900 mb-2">Контроль якості не пройдено</h3>
            <p className="text-gray-600 text-sm mb-5">Оберіть наступну дію:</p>
            <div className="space-y-2">
              <button
                onClick={() => { setShowDecision(false); onRework(); }}
                className="w-full text-left border border-gray-300 rounded p-4 hover:bg-gray-50"
              >
                <div className="text-sm font-medium text-gray-800">↩ Повернути на доопрацювання</div>
                <div className="text-xs text-gray-500 mt-1">Виріб повертається на виробничий етап для корекції</div>
              </button>
              <button
                onClick={() => { setShowDecision(false); onFail(); }}
                className="w-full text-left border border-gray-300 rounded p-4 hover:bg-gray-50"
              >
                <div className="text-sm font-medium text-gray-800">✗ Позначити процес як провалений</div>
                <div className="text-xs text-gray-500 mt-1">Процес закривається, створюється знімок стану</div>
              </button>
            </div>
            <button onClick={() => setShowDecision(false)} className="mt-3 w-full text-center text-sm text-gray-400 hover:text-gray-600">Скасувати</button>
          </div>
        </div>
      )}
    </div>
  );
}
