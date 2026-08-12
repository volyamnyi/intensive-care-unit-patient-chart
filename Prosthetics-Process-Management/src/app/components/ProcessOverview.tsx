import { ArrowLeft, Play, GitBranch } from "lucide-react";
import { Process, ProcessTemplate, Patient, Order } from "./mockData";

interface Props {
  process: Process;
  template: ProcessTemplate;
  patient: Patient;
  order: Order;
  onStart: () => void;
  onBack: () => void;
}

export function ProcessOverview({ process, template, patient, order, onStart, onBack }: Props) {
  const stages = template.stages;
  const totalSteps = stages.reduce((sum, s) => sum + s.steps.length, 0);

  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={onBack} className="flex items-center gap-1 text-gray-500 hover:text-gray-800 text-sm">
            <ArrowLeft size={16} /> Назад
          </button>
          <div className="text-gray-300">|</div>
          <h1 className="text-gray-900">Огляд технологічного процесу</h1>
        </div>

        <div className="grid grid-cols-[220px_1fr_220px] gap-4 h-[calc(100vh-220px)]">
          {/* Left sidebar - process tree */}
          <div className="bg-white border border-gray-200 rounded overflow-auto">
            <div className="px-4 py-3 border-b border-gray-200">
              <h3 className="text-sm text-gray-700">Структура процесу</h3>
            </div>
            <div className="p-3">
              {stages.map((stage, si) => (
                <div key={stage.id} className="mb-3">
                  <div className={`flex items-center gap-2 text-xs font-medium px-2 py-1.5 rounded ${stage.type === "quality" ? "bg-gray-800 text-white" : stage.type === "clinical" ? "bg-gray-200 text-gray-700" : "bg-gray-100 text-gray-700"}`}>
                    {stage.type === "quality" ? "◆" : stage.type === "clinical" ? "●" : "■"}
                    <span>{stage.name}</span>
                  </div>
                  <div className="ml-4 mt-1 space-y-0.5">
                    {stage.steps.map((step, ti) => (
                      <div key={step.id} className="flex items-center gap-1.5 text-xs text-gray-500 py-0.5">
                        <span className="w-3 h-px bg-gray-300" />
                        <span>{step.name}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
            <div className="px-4 py-3 border-t border-gray-100 text-xs text-gray-400">
              <div className="flex items-center gap-2 mb-1"><span className="bg-gray-200 text-gray-700 px-1.5 py-0.5 rounded">●</span>Клінічний</div>
              <div className="flex items-center gap-2 mb-1"><span className="bg-gray-100 text-gray-700 px-1.5 py-0.5 rounded">■</span>Виробничий</div>
              <div className="flex items-center gap-2"><span className="bg-gray-800 text-white px-1.5 py-0.5 rounded">◆</span>Контроль якості</div>
            </div>
          </div>

          {/* Center - workflow diagram */}
          <div className="bg-white border border-gray-200 rounded overflow-auto">
            <div className="px-4 py-3 border-b border-gray-200">
              <h3 className="text-sm text-gray-700">Технологічна карта (BPMN-inspired)</h3>
            </div>
            <div className="p-6">
              {/* Workflow visualization */}
              <div className="flex flex-col items-center gap-0">
                {/* Start */}
                <div className="w-10 h-10 rounded-full bg-gray-900 border-2 border-gray-900 flex items-center justify-center">
                  <span className="text-white text-xs">▶</span>
                </div>
                <div className="w-px h-4 bg-gray-400" />

                {stages.map((stage, si) => (
                  <div key={stage.id} className="flex flex-col items-center w-full">
                    {stage.type === "quality" ? (
                      /* Diamond for quality gates */
                      <div className="relative flex items-center justify-center my-1">
                        <div className="w-16 h-16 bg-gray-100 border-2 border-gray-700 rotate-45 flex items-center justify-center" />
                        <div className="absolute text-center">
                          <span className="text-xs font-medium text-gray-800 leading-tight" style={{ transform: "none" }}>QC</span>
                        </div>
                        <div className="absolute -bottom-6 text-xs text-gray-600 w-32 text-center">{stage.name}</div>
                        {/* Rework arrow */}
                        <div className="absolute left-full ml-2 flex items-center text-xs text-gray-400">
                          <span className="border border-dashed border-gray-300 rounded px-2 py-1 ml-2 text-gray-400">↩ Rework</span>
                        </div>
                      </div>
                    ) : (
                      /* Rectangle for regular stages */
                      <div className={`w-56 rounded border-2 px-4 py-2 text-center ${si < 2 ? "bg-gray-100 border-gray-400" : "bg-gray-50 border-gray-300"}`}>
                        <div className="text-xs font-medium text-gray-800">{stage.name}</div>
                        <div className="text-xs text-gray-500 mt-0.5">{stage.steps.length} кроків</div>
                      </div>
                    )}
                    {si < stages.length - 1 && <div className="w-px h-6 bg-gray-400 mt-2" />}
                  </div>
                ))}

                <div className="w-px h-4 bg-gray-400 mt-2" />
                {/* End */}
                <div className="w-10 h-10 rounded-full bg-gray-900 border-4 border-gray-700 flex items-center justify-center">
                  <div className="w-4 h-4 rounded-full bg-gray-900 border-2 border-white" />
                </div>
              </div>
            </div>
          </div>

          {/* Right sidebar - metadata */}
          <div className="bg-white border border-gray-200 rounded overflow-auto">
            <div className="px-4 py-3 border-b border-gray-200">
              <h3 className="text-sm text-gray-700">Метадані процесу</h3>
            </div>
            <div className="p-4 space-y-3 text-sm">
              <div><span className="text-gray-400 text-xs block">Процес</span><span className="text-gray-800">{template.name}</span></div>
              <div><span className="text-gray-400 text-xs block">Пацієнт</span><span className="text-gray-800">{patient.name}</span></div>
              <div><span className="text-gray-400 text-xs block">Замовлення</span><span className="text-gray-700 font-mono">{order.orderNumber}</span></div>
              <div><span className="text-gray-400 text-xs block">Призначений</span><span className="text-gray-800">{process.assignedUser}</span></div>
              <div><span className="text-gray-400 text-xs block">Статус</span><span className="bg-gray-100 text-gray-700 border border-gray-300 px-2 py-0.5 rounded text-xs">{process.status}</span></div>
              <div><span className="text-gray-400 text-xs block">Тривалість</span><span className="text-gray-700">{template.estimatedDuration}</span></div>
              <div className="border-t border-gray-100 pt-3">
                <span className="text-gray-400 text-xs block mb-1">Прогрес</span>
                <div className="flex items-center gap-2">
                  <div className="flex-1 h-2 bg-gray-200 rounded-full">
                    <div className="h-2 bg-gray-700 rounded-full w-0" />
                  </div>
                  <span className="text-xs text-gray-500">0%</span>
                </div>
                <div className="text-xs text-gray-400 mt-1">0 / {totalSteps} кроків</div>
              </div>
            </div>
          </div>
        </div>

        <div className="flex justify-between mt-4">
          <button onClick={onBack} className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-700 hover:bg-gray-100">
            Назад
          </button>
          <button onClick={onStart} className="flex items-center gap-2 bg-gray-800 text-white px-6 py-2 rounded text-sm hover:bg-gray-700">
            <Play size={16} /> Розпочати процес
          </button>
        </div>
      </div>
    </div>
  );
}
