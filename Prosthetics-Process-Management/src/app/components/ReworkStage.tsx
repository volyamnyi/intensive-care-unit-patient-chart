import { RotateCcw, Eye } from "lucide-react";
import { Process, ProcessTemplate } from "./mockData";

interface Props {
  process: Process;
  template: ProcessTemplate;
  onStartRework: () => void;
  onMainMenu: () => void;
}

export function ReworkStage({ process, template, onStartRework, onMainMenu }: Props) {
  const correctiveActions = [
    "Перевірити форму та відповідність розмірів технічним вимогам",
    "Виконати повторну обробку поверхні для усунення дефектів",
    "Перевірити якість матеріалу та при необхідності замінити",
    "Повторно виміряти всі критичні параметри після корекції",
  ];

  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-3xl mx-auto">
        <div className="bg-gray-700 text-white rounded px-5 py-3 mb-5 flex items-center gap-3">
          <span className="text-xl">↩</span>
          <div>
            <div className="text-xs text-gray-300 mb-0.5">Стадія доопрацювання</div>
            <h2 className="text-white">Повернення на виробничий етап</h2>
          </div>
          <div className="ml-auto text-xs text-gray-400">{process.patientName}</div>
        </div>

        <div className="space-y-4">
          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-3">Провалена контрольна точка</h3>
            <div className="bg-gray-50 border border-gray-200 rounded p-4 space-y-2 text-sm">
              <div className="flex gap-3"><span className="text-gray-400 w-28 shrink-0">Контрольна точка:</span><span className="text-gray-800">Контроль якості гільзи</span></div>
              <div className="flex gap-3"><span className="text-gray-400 w-28 shrink-0">Причина відмови:</span><span className="text-gray-800">Розміри виходять за межі допуску, виявлено мікротріщини на поверхні</span></div>
              <div className="flex gap-3"><span className="text-gray-400 w-28 shrink-0">Дата перевірки:</span><span className="text-gray-800">2024-03-15 11:30</span></div>
              <div className="flex gap-3"><span className="text-gray-400 w-28 shrink-0">Перевіряючий:</span><span className="text-gray-800">{process.assignedUser}</span></div>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-3">Необхідні коригуючі дії</h3>
            <div className="space-y-2">
              {correctiveActions.map((action, i) => (
                <div key={i} className="flex items-start gap-3 p-3 border border-gray-100 rounded bg-gray-50">
                  <span className="w-6 h-6 bg-gray-200 rounded-full flex items-center justify-center text-xs text-gray-600 shrink-0 mt-0.5">{i + 1}</span>
                  <span className="text-sm text-gray-700">{action}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-2">Маршрут доопрацювання</h3>
            <p className="text-gray-600 text-sm mb-3">Система автоматично перенаправить вас на початок виробничого етапу для повторного виконання необхідних кроків.</p>
            <div className="flex items-center gap-2 text-sm">
              <span className="bg-gray-800 text-white px-2.5 py-1 rounded text-xs">◆ КЯ: Гільза</span>
              <span className="text-gray-400">←</span>
              <span className="bg-gray-100 text-gray-700 border border-gray-200 px-2.5 py-1 rounded text-xs">■ Виготовлення гільзи</span>
              <span className="text-gray-400 text-xs">(крок 2)</span>
            </div>
          </div>

          <div className="flex gap-3">
            <button
              onClick={onStartRework}
              className="flex items-center gap-2 bg-gray-800 text-white px-6 py-2.5 rounded text-sm hover:bg-gray-700"
            >
              <RotateCcw size={16} /> Розпочати доопрацювання
            </button>
            <button className="flex items-center gap-2 border border-gray-300 text-gray-700 px-4 py-2.5 rounded text-sm hover:bg-gray-50">
              <Eye size={16} /> Переглянути попередні результати
            </button>
            <button onClick={onMainMenu} className="ml-auto border border-gray-200 text-gray-500 px-4 py-2.5 rounded text-sm hover:bg-gray-50">
              Головна
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
