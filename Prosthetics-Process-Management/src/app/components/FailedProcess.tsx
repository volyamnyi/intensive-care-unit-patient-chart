import { ArrowLeft, FileText, Plus, Eye } from "lucide-react";
import { Process } from "./mockData";

interface Props {
  process: Process;
  onViewSnapshot: () => void;
  onCreateReplacement: () => void;
  onBack: () => void;
}

const FAILURE_CATEGORIES = [
  "Виробничий дефект",
  "Проблема з матеріалом",
  "Повторна відмова контролю якості",
  "Пошкоджений компонент",
  "Неправильне замовлення",
  "Відмова пацієнта",
  "Скасування процесу",
];

export function FailedProcess({ process, onViewSnapshot, onCreateReplacement, onBack }: Props) {
  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-3xl mx-auto">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={onBack} className="flex items-center gap-1 text-gray-500 hover:text-gray-800 text-sm">
            <ArrowLeft size={16} /> Назад до дашборду
          </button>
        </div>

        <div className="bg-gray-800 text-white rounded px-5 py-4 mb-5">
          <div className="flex items-center gap-3">
            <span className="text-2xl">✗</span>
            <div>
              <div className="text-xs text-gray-400 mb-0.5">Процес завершено з помилкою</div>
              <h2 className="text-white">{process.id}</h2>
            </div>
            <div className="ml-auto text-right">
              <div className="text-xs text-gray-400">Пацієнт</div>
              <div className="text-sm">{process.patientName}</div>
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-4">Деталі відмови</h3>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-gray-400 text-xs block mb-1">Причина відмови</span>
                <p className="text-gray-800">Виріб не відповідає критеріям якості після двох повторних перевірок. Виявлено структурні дефекти матеріалу.</p>
              </div>
              <div>
                <span className="text-gray-400 text-xs block mb-1">Категорія відмови</span>
                <span className="bg-gray-100 text-gray-700 border border-gray-300 px-2 py-1 rounded text-xs">Виробничий дефект</span>
              </div>
              <div>
                <span className="text-gray-400 text-xs block mb-1">Провалений етап</span>
                <span className="text-gray-800">Контроль якості гільзи</span>
              </div>
              <div>
                <span className="text-gray-400 text-xs block mb-1">Провалений крок</span>
                <span className="text-gray-800">Перевірка якості (спроба 2)</span>
              </div>
              <div>
                <span className="text-gray-400 text-xs block mb-1">Відповідальний</span>
                <span className="text-gray-800">{process.assignedUser}</span>
              </div>
              <div>
                <span className="text-gray-400 text-xs block mb-1">Час відмови</span>
                <span className="text-gray-800">{process.lastUpdated}</span>
              </div>
            </div>
          </div>

          <div className="bg-gray-50 border border-gray-200 rounded p-4 text-sm">
            <div className="flex items-start gap-2">
              <span className="text-gray-500 mt-0.5">ℹ</span>
              <p className="text-gray-600">Система автоматично створила незмінний <strong>Знімок стану</strong> цього процесу. Знімок фіксує всі дані, матеріали та журнал подій на момент відмови.</p>
            </div>
          </div>

          <div className="flex gap-3 flex-wrap">
            <button onClick={onViewSnapshot} className="flex items-center gap-2 bg-gray-800 text-white px-4 py-2.5 rounded text-sm hover:bg-gray-700">
              <Eye size={16} /> Переглянути знімок стану
            </button>
            <button className="flex items-center gap-2 border border-gray-300 text-gray-700 px-4 py-2.5 rounded text-sm hover:bg-gray-50">
              <FileText size={16} /> Експортувати звіт про відмову
            </button>
            <button onClick={onCreateReplacement} className="flex items-center gap-2 border border-gray-300 text-gray-700 px-4 py-2.5 rounded text-sm hover:bg-gray-50">
              <Plus size={16} /> Створити замінний процес
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
