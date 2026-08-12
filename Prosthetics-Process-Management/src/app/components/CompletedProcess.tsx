import { CheckCircle, FileText, ArrowLeft } from "lucide-react";
import { Process, ProcessTemplate } from "./mockData";

interface Props {
  process: Process;
  template: ProcessTemplate;
  onMainMenu: () => void;
}

export function CompletedProcess({ process, template, onMainMenu }: Props) {
  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-4xl mx-auto">
        <div className="bg-gray-900 text-white rounded px-5 py-4 mb-5 flex items-center gap-3">
          <CheckCircle size={28} className="text-gray-300" />
          <div>
            <div className="text-xs text-gray-400 mb-0.5">Процес успішно завершено</div>
            <h2 className="text-white">{process.id}</h2>
          </div>
          <div className="ml-auto text-right">
            <div className="text-xs text-gray-400">Завершено</div>
            <div className="text-sm">{process.lastUpdated}</div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-white border border-gray-200 rounded p-5">
              <h3 className="text-gray-700 text-sm mb-3">Інформація про процес</h3>
              <div className="space-y-2 text-sm">
                {[["ID", process.id], ["Шаблон", template.name], ["Розпочато", process.startDate], ["Завершено", process.lastUpdated], ["Виконавець", process.assignedUser]].map(([k, v]) => (
                  <div key={k} className="flex justify-between border-b border-gray-50 pb-1">
                    <span className="text-gray-400">{k}</span>
                    <span className="text-gray-800">{v}</span>
                  </div>
                ))}
              </div>
            </div>
            <div className="bg-white border border-gray-200 rounded p-5">
              <h3 className="text-gray-700 text-sm mb-3">Інформація про пацієнта та замовлення</h3>
              <div className="space-y-2 text-sm">
                {[["Пацієнт", process.patientName], ["Замовлення", process.orderNumber], ["Тип протезу", "Протез нижньої кінцівки (транстибіальний)"]].map(([k, v]) => (
                  <div key={k} className="border-b border-gray-50 pb-1">
                    <span className="text-gray-400 text-xs block">{k}</span>
                    <span className="text-gray-800">{v}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-4">Завершені етапи</h3>
            <div className="space-y-2">
              {template.stages.map((stage, i) => (
                <div key={stage.id} className="flex items-center gap-3 p-3 bg-gray-50 rounded">
                  <CheckCircle size={16} className="text-gray-600 shrink-0" />
                  <div>
                    <div className="text-sm text-gray-800">{stage.name}</div>
                    <div className="text-xs text-gray-500">{stage.steps.length} кроків завершено</div>
                  </div>
                  <div className="ml-auto text-xs text-gray-400">
                    {stage.type === "quality" ? "◆ Пройдено" : `✓ Завершено`}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-3">Результати контролю якості</h3>
            <div className="space-y-2">
              {["Контроль якості гільзи", "Фінальна перевірка збірки"].map(qc => (
                <div key={qc} className="flex items-center justify-between p-3 border border-gray-100 rounded">
                  <span className="text-sm text-gray-700">{qc}</span>
                  <span className="text-xs text-gray-600 bg-gray-100 px-2 py-0.5 rounded border border-gray-200">✓ Пройдено</span>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-2">Підсумковий результат</h3>
            <div className="bg-gray-50 rounded p-4 text-sm text-gray-700 leading-relaxed">
              Протез нижньої кінцівки успішно виготовлено і підігнано. Всі контрольні точки якості пройдено. Пацієнт пройшов навчання та підписав акт прийому-передачі. Виріб готовий до використання.
            </div>
          </div>

          <div className="flex gap-3">
            <button className="flex items-center gap-2 bg-gray-800 text-white px-4 py-2.5 rounded text-sm hover:bg-gray-700">
              <FileText size={16} /> Експортувати PDF
            </button>
            <button onClick={onMainMenu} className="ml-auto border border-gray-200 text-gray-500 px-4 py-2.5 rounded text-sm hover:bg-gray-50">
              Повернутись до дашборду
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
