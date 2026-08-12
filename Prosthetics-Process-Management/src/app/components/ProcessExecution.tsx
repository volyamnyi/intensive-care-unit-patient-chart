import { useState } from "react";
import { Home, PauseCircle, ChevronLeft, ChevronRight, Save } from "lucide-react";
import { Process, ProcessTemplate, Stage, Step } from "./mockData";

interface Props {
  process: Process;
  template: ProcessTemplate;
  onComplete: () => void;
  onQualityCheck: () => void;
  onPause: () => void;
  onMainMenu: () => void;
  stageIndex: number;
  stepIndex: number;
  onUpdateProgress: (stageIndex: number, stepIndex: number) => void;
}

export function ProcessExecution({ process, template, onComplete, onQualityCheck, onPause, onMainMenu, stageIndex, stepIndex, onUpdateProgress }: Props) {
  const [formValues, setFormValues] = useState<Record<string, string>>({});
  const [materials, setMaterials] = useState([{ name: "", qty: "", unit: "" }]);
  const [laborTime, setLaborTime] = useState("");
  const [notes, setNotes] = useState("");

  const stages = template.stages;
  const currentStage = stages[stageIndex];
  const currentStep = currentStage?.steps[stepIndex];

  if (!currentStage || !currentStep) {
    return <div className="flex-1 flex items-center justify-center text-gray-500">Немає кроків</div>;
  }

  const totalSteps = stages.reduce((sum, s) => sum + s.steps.length, 0);
  const completedSteps = stages.slice(0, stageIndex).reduce((sum, s) => sum + s.steps.length, 0) + stepIndex;
  const progress = Math.round((completedSteps / totalSteps) * 100);

  const handleNext = () => {
    if (stepIndex < currentStage.steps.length - 1) {
      onUpdateProgress(stageIndex, stepIndex + 1);
    } else if (stageIndex < stages.length - 1) {
      const nextStage = stages[stageIndex + 1];
      if (nextStage.type === "quality") {
        onQualityCheck();
      } else {
        onUpdateProgress(stageIndex + 1, 0);
      }
    } else {
      onComplete();
    }
    setFormValues({});
    setNotes("");
  };

  const handlePrev = () => {
    if (stepIndex > 0) {
      onUpdateProgress(stageIndex, stepIndex - 1);
    } else if (stageIndex > 0) {
      const prevStage = stages[stageIndex - 1];
      onUpdateProgress(stageIndex - 1, prevStage.steps.length - 1);
    }
  };

  const isLastStep = stageIndex === stages.length - 1 && stepIndex === currentStage.steps.length - 1;
  const isBeforeQuality = stepIndex === currentStage.steps.length - 1 && stageIndex < stages.length - 1 && stages[stageIndex + 1].type === "quality";

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-gray-50">
      <div className="bg-white border-b border-gray-200 px-6 py-3 shrink-0">
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2 text-sm">
              <span className="text-gray-800 font-medium">{template.name}</span>
              <span className="text-gray-400">•</span>
              <span className="text-gray-600">{process.patientName}</span>
              <span className="text-gray-400">•</span>
              <span className="text-gray-500 font-mono text-xs">{process.orderNumber}</span>
            </div>
            <div className="flex items-center gap-4 mt-1">
              <div className="flex items-center gap-2 text-xs text-gray-500">
                <span>Прогрес:</span>
                <div className="w-40 h-1.5 bg-gray-200 rounded-full">
                  <div className="h-1.5 bg-gray-700 rounded-full transition-all" style={{ width: `${progress}%` }} />
                </div>
                <span>{progress}%</span>
              </div>
              <span className="text-xs text-gray-500">Етап: <strong>{currentStage.name}</strong></span>
              <span className="text-xs text-gray-500">Крок: <strong>{currentStep.name}</strong></span>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={onPause} className="flex items-center gap-1.5 border border-gray-300 rounded px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-50">
              <PauseCircle size={14} /> Пауза
            </button>
            <button onClick={onMainMenu} className="flex items-center gap-1.5 border border-gray-300 rounded px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-50">
              <Home size={14} /> Головна
            </button>
          </div>
        </div>
        <div className="flex items-center gap-1 mt-3 overflow-x-auto pb-1">
          {stages.map((stage, si) => (
            <div key={stage.id} className="flex items-center gap-1 shrink-0">
              <div className={`px-2.5 py-1 rounded text-xs whitespace-nowrap ${si < stageIndex ? "bg-gray-700 text-white" : si === stageIndex ? "bg-gray-800 text-white border-2 border-gray-600" : "bg-gray-100 text-gray-500 border border-gray-200"}`}>
                {stage.type === "quality" && "◆ "}{stage.name}
              </div>
              {si < stages.length - 1 && <span className="text-gray-300 text-xs">→</span>}
            </div>
          ))}
        </div>
      </div>

      <div className="flex-1 overflow-auto p-6">
        <div className="max-w-4xl mx-auto space-y-4">
          <div className="bg-white border border-gray-200 rounded p-5">
            <div className="flex items-start justify-between">
              <div>
                <div className="text-xs text-gray-400 mb-1">Крок {completedSteps + 1} з {totalSteps} — {currentStage.name}</div>
                <h2 className="text-gray-900">{currentStep.name}</h2>
                <p className="text-gray-600 text-sm mt-2 leading-relaxed">{currentStep.instruction}</p>
              </div>
              <span className={`px-2 py-1 rounded text-xs border ml-4 shrink-0 ${currentStage.type === "quality" ? "bg-gray-800 text-white" : currentStage.type === "clinical" ? "bg-gray-200 text-gray-700 border-gray-300" : "bg-gray-100 text-gray-600 border-gray-200"}`}>
                {currentStage.type === "quality" ? "Контроль якості" : currentStage.type === "clinical" ? "Клінічний" : "Виробничий"}
              </span>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-4">Перевірка виконання етапу</h3>
            <div className="space-y-3">
              {[
                "гіпсовий негатив виготовлено",
                "гіпсовий негатив перевірено на",
                "гіпсовий позитив виготовлено",
                "гіпсовий позитив перевірено на",
                "тестову гільзу перевірено на фідповідність фактичним антропометричним",
                "комплектацію сформовано (лист для збірки комплектації на склад)",
                "прототип зібрано і передано ерготерапевту для верифікації з пацієнтом",
                "гільза сформована",
                "постійну внутрішню гільзу перевірено на фідповідність фактичним",
                "заклепки на протезі щільно підтягнуті, обтиснуті до",
                "шарнірні з'єднання забезпечують безшумне, легке,",
                "всі компоненти надійно з'єднани між собою, протез",
                "кріплення надійно зафіксовано на протезі",
                "перевірено екскурсію тяг та спрацьовування",
                "при ходьбі протез здійснює вільні коливання у ліктьовому шарнірі вузла",
                "пацієнт знає принцип дії протеза з тяговим керуванням",
                "пацієнт може розфіксувати та зафіксувати шарнірний вузол “лікоть-",
                "на протез нанесено маркування."
              ].map((item, idx) => (
                <label key={idx} className="flex items-center gap-3 cursor-pointer p-2 hover:bg-gray-50 rounded border border-transparent hover:border-gray-200 transition-colors">
                  <input type="checkbox" className="rounded border-gray-400 w-4 h-4" />
                  <span className="text-sm text-gray-600">{item}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-4">Облік ресурсів</h3>
            <div className="space-y-3">
              <div>
                <h4 className="text-xs text-gray-500 uppercase tracking-wider mb-2">Матеріали</h4>
                <table className="w-full text-sm border border-gray-200 rounded">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Матеріал</th>
                      <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Кількість</th>
                      <th className="text-left px-3 py-2 text-xs text-gray-500 font-medium">Одиниця</th>
                      <th className="px-2 py-2"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {materials.map((m, i) => (
                      <tr key={i} className="border-t border-gray-100">
                        <td className="px-2 py-1"><input className="w-full border border-gray-200 rounded px-2 py-1 text-xs" placeholder="Назва матеріалу" value={m.name} onChange={e => { const upd = [...materials]; upd[i].name = e.target.value; setMaterials(upd); }} /></td>
                        <td className="px-2 py-1"><input className="w-20 border border-gray-200 rounded px-2 py-1 text-xs" type="number" placeholder="0" value={m.qty} onChange={e => { const upd = [...materials]; upd[i].qty = e.target.value; setMaterials(upd); }} /></td>
                        <td className="px-2 py-1"><select className="border border-gray-200 rounded px-2 py-1 text-xs"><option>кг</option><option>г</option><option>л</option><option>мл</option><option>шт</option><option>м</option></select></td>
                        <td className="px-2 py-1"><button className="text-gray-400 hover:text-gray-600 text-xs" onClick={() => setMaterials(materials.filter((_, j) => j !== i))}>✕</button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <button className="mt-2 text-xs text-gray-500 hover:text-gray-700 border border-dashed border-gray-300 rounded px-3 py-1" onClick={() => setMaterials([...materials, { name: "", qty: "", unit: "" }])}>+ Додати матеріал</button>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs text-gray-500">Трудовитрати (хв)</label>
                  <input type="number" className="w-full mt-1 border border-gray-300 rounded px-3 py-1.5 text-sm" placeholder="0" value={laborTime} onChange={e => setLaborTime(e.target.value)} />
                </div>
                <div>
                  <label className="text-xs text-gray-500">Примітки</label>
                  <textarea className="w-full mt-1 border border-gray-300 rounded px-3 py-1.5 text-sm resize-none h-9" placeholder="Додаткові примітки..." value={notes} onChange={e => setNotes(e.target.value)} />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white border-t border-gray-200 px-6 py-3 shrink-0 flex items-center justify-between">
        <div className="flex gap-2">
          <button onClick={handlePrev} disabled={stageIndex === 0 && stepIndex === 0} className="flex items-center gap-1 px-3 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed">
            <ChevronLeft size={14} /> Попередній
          </button>
          <button className="flex items-center gap-1.5 px-3 py-2 border border-gray-300 rounded text-sm text-gray-600 hover:bg-gray-50">
            <Save size={14} /> Зберегти чернетку
          </button>
        </div>
        <button onClick={handleNext} className="flex items-center gap-1.5 bg-gray-800 text-white px-6 py-2 rounded text-sm hover:bg-gray-700">
          {isLastStep ? "Завершити процес" : isBeforeQuality ? "Контроль якості →" : "Готово →"}
          {!isLastStep && <ChevronRight size={14} />}
        </button>
      </div>
    </div>
  );
}