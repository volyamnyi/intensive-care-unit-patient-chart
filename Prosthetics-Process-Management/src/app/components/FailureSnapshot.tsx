import { ArrowLeft, Lock, FileText, Printer } from "lucide-react";
import { Process } from "./mockData";

interface Props {
  process: Process;
  onBack: () => void;
  onMainMenu: () => void;
}

export function FailureSnapshot({ process, onBack, onMainMenu }: Props) {
  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center gap-3 mb-4">
          <button onClick={onBack} className="flex items-center gap-1 text-gray-500 hover:text-gray-800 text-sm">
            <ArrowLeft size={16} /> Назад
          </button>
          <div className="text-gray-300">|</div>
          <div>
            <h1 className="text-gray-900">Знімок стану процесу</h1>
            <p className="text-gray-500 text-sm">{process.id}</p>
          </div>
          <div className="ml-auto flex items-center gap-2 bg-gray-100 border border-gray-300 rounded px-3 py-1.5 text-xs text-gray-600">
            <Lock size={12} />
            Незмінний запис — лише для читання
          </div>
        </div>

        <div className="space-y-4">
          {/* Process info */}
          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-4">Інформація про процес</h3>
            <div className="grid grid-cols-3 gap-4 text-sm">
              {[
                ["ID процесу", process.id],
                ["Шаблон процесу", process.templateName],
                ["Пацієнт", process.patientName],
                ["Замовлення", process.orderNumber],
                ["Дата відмови", process.lastUpdated],
                ["Причина відмови", "Виробничий дефект — структурні тріщини"],
              ].map(([label, value]) => (
                <div key={label}>
                  <span className="text-gray-400 text-xs block">{label}</span>
                  <span className="text-gray-800">{value}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            {/* Operational data */}
            <div className="bg-white border border-gray-200 rounded p-5">
              <h3 className="text-gray-700 text-sm mb-3">Операційні дані</h3>
              <div className="space-y-2 text-sm">
                {[
                  ["Завершені етапи", "Клінічна оцінка, Виготовлення гільзи (частково)"],
                  ["Завершені кроки", "5 з 11"],
                  ["Активності користувача", "23 операції"],
                ].map(([label, value]) => (
                  <div key={label} className="border-b border-gray-100 pb-2">
                    <span className="text-gray-400 text-xs block">{label}</span>
                    <span className="text-gray-700">{value}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Resource consumption */}
            <div className="bg-white border border-gray-200 rounded p-5">
              <h3 className="text-gray-700 text-sm mb-3">Витрачені ресурси</h3>
              <table className="w-full text-xs">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-1 text-gray-400">Матеріал</th>
                    <th className="text-right py-1 text-gray-400">Кількість</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {[
                    ["Скловолокно", "1.2 кг"],
                    ["Ламінаційна смола", "0.8 л"],
                    ["Поліетиленовий чохол", "1 шт"],
                  ].map(([mat, qty]) => (
                    <tr key={mat}>
                      <td className="py-1.5 text-gray-700">{mat}</td>
                      <td className="py-1.5 text-right text-gray-600">{qty}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="mt-3 pt-2 border-t border-gray-100 text-xs space-y-1">
                <div className="flex justify-between text-gray-600"><span>Трудовитрати</span><span>14.5 год</span></div>
                <div className="flex justify-between text-gray-600"><span>Час обладнання</span><span>3.2 год</span></div>
              </div>
            </div>
          </div>

          {/* Metrics */}
          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-3">Показники</h3>
            <div className="grid grid-cols-4 gap-4 text-sm">
              {[
                ["Планова тривалість", "14 днів", ""],
                ["Фактична тривалість", "5 днів", ""],
                ["Точка відмови", "Етап 3, Крок 1", ""],
                ["Кількість доопрацювань", "2", ""],
              ].map(([label, value]) => (
                <div key={label} className="bg-gray-50 rounded p-3">
                  <span className="text-gray-400 text-xs block mb-1">{label}</span>
                  <span className="text-gray-800">{value}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Timeline */}
          <div className="bg-white border border-gray-200 rounded p-5">
            <h3 className="text-gray-700 text-sm mb-4">Хронологія</h3>
            <div className="relative">
              <div className="absolute left-4 top-0 bottom-0 w-px bg-gray-200" />
              <div className="space-y-3">
                {[
                  ["2024-03-07 09:00", "Процес розпочато"],
                  ["2024-03-07 09:30", "Клінічна оцінка — розпочато"],
                  ["2024-03-08 16:00", "Клінічна оцінка — завершено"],
                  ["2024-03-09 08:30", "Виготовлення гільзи — розпочато"],
                  ["2024-03-10 10:00", "КЯ Гільза — не пройдено (спроба 1)"],
                  ["2024-03-10 10:30", "Доопрацювання — розпочато"],
                  ["2024-03-10 14:00", "КЯ Гільза — не пройдено (спроба 2)"],
                  ["2024-03-10 11:00", "Процес позначено як провалений"],
                ].map(([time, desc]) => (
                  <div key={time} className="flex gap-4 pl-2">
                    <div className="w-4 h-4 rounded-full bg-gray-300 border border-white z-10 mt-0.5 shrink-0" />
                    <div className="text-sm">
                      <span className="text-gray-500 text-xs">{time}</span>
                      <div className="text-gray-700">{desc}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="flex gap-3">
            <button className="flex items-center gap-2 bg-gray-800 text-white px-4 py-2.5 rounded text-sm hover:bg-gray-700">
              <FileText size={16} /> Експортувати PDF
            </button>
            <button className="flex items-center gap-2 border border-gray-300 text-gray-700 px-4 py-2.5 rounded text-sm hover:bg-gray-50">
              <Printer size={16} /> Роздрукувати
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
