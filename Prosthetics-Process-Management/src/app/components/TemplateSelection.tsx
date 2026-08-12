import { ArrowLeft, Clock, Layers } from "lucide-react";
import { MOCK_TEMPLATES, ProcessTemplate } from "./mockData";

interface Props {
  onSelect: (template: ProcessTemplate) => void;
  onBack: () => void;
}

export function TemplateSelection({ onSelect, onBack }: Props) {
  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={onBack} className="flex items-center gap-1 text-gray-500 hover:text-gray-800 text-sm">
            <ArrowLeft size={16} /> Назад
          </button>
          <div className="text-gray-300">|</div>
          <div>
            <h1 className="text-gray-900">Вибір шаблону процесу</h1>
            <p className="text-gray-500 text-sm">Крок 4 з 4 — Оберіть технологічний процес для виконання</p>
          </div>
        </div>

        {/* Progress indicator */}
        <div className="flex items-center gap-0 mb-6">
          {["Пацієнт", "Замовлення", "Перегляд", "Шаблон"].map((step, i) => (
            <div key={i} className="flex items-center">
              <div className={`flex items-center gap-2 px-3 py-1.5 text-xs ${i === 3 ? "bg-gray-800 text-white rounded" : "text-gray-600"}`}>
                <span className={`w-5 h-5 rounded-full border flex items-center justify-center ${i === 3 ? "bg-white text-gray-800 border-white" : "bg-gray-600 text-white border-gray-600"}`}>{i < 3 ? "✓" : i + 1}</span>
                {step}
              </div>
              {i < 3 && <div className="w-8 h-px bg-gray-300" />}
            </div>
          ))}
        </div>

        <p className="text-gray-500 text-sm mb-4">Шаблони завантажуються динамічно з бази даних. Оберіть відповідний шаблон для типу протезу.</p>

        <div className="grid grid-cols-1 gap-3">
          {MOCK_TEMPLATES.map(t => (
            <div key={t.id} className="bg-white border border-gray-200 rounded p-5 hover:border-gray-400 hover:shadow-sm transition-all cursor-pointer" onClick={() => onSelect(t)}>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <h3 className="text-gray-900">{t.name}</h3>
                  <p className="text-gray-500 text-sm mt-1">{t.description}</p>
                  <div className="flex items-center gap-5 mt-3">
                    <div className="flex items-center gap-1.5 text-xs text-gray-500">
                      <Clock size={13} />
                      <span>Тривалість: {t.estimatedDuration}</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-xs text-gray-500">
                      <Layers size={13} />
                      <span>Етапів: {t.stageCount}</span>
                    </div>
                  </div>
                </div>
                <button
                  onClick={(e) => { e.stopPropagation(); onSelect(t); }}
                  className="ml-4 bg-gray-800 text-white px-4 py-2 rounded text-sm hover:bg-gray-700 whitespace-nowrap"
                >
                  Обрати процес
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
