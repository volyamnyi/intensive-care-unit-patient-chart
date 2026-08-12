import { ArrowLeft } from "lucide-react";
import { Patient, Order, MOCK_ORDERS } from "./mockData";

interface Props {
  patient: Patient;
  onSelect: (order: Order) => void;
  onBack: () => void;
}

export function OrderSelection({ patient, onSelect, onBack }: Props) {
  const orders = MOCK_ORDERS.filter(o => o.patientId === patient.id);

  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={onBack} className="flex items-center gap-1 text-gray-500 hover:text-gray-800 text-sm">
            <ArrowLeft size={16} /> Назад
          </button>
          <div className="text-gray-300">|</div>
          <div>
            <h1 className="text-gray-900">Вибір замовлення</h1>
            <p className="text-gray-500 text-sm">Крок 2 з 4 — Оберіть замовлення для пацієнта</p>
          </div>
        </div>

        {/* Progress indicator */}
        <div className="flex items-center gap-0 mb-6">
          {["Пацієнт", "Замовлення", "Перегляд", "Шаблон"].map((step, i) => (
            <div key={i} className="flex items-center">
              <div className={`flex items-center gap-2 px-3 py-1.5 text-xs ${i === 1 ? "bg-gray-800 text-white rounded" : i < 1 ? "text-gray-600" : "text-gray-400"}`}>
                <span className={`w-5 h-5 rounded-full border flex items-center justify-center ${i === 1 ? "bg-white text-gray-800 border-white" : i < 1 ? "bg-gray-600 text-white border-gray-600" : "border-gray-300"}`}>{i < 1 ? "✓" : i + 1}</span>
                {step}
              </div>
              {i < 3 && <div className="w-8 h-px bg-gray-300" />}
            </div>
          ))}
        </div>

        {/* Patient summary */}
        <div className="bg-gray-100 border border-gray-200 rounded p-4 mb-4">
          <h3 className="text-gray-700 text-sm mb-2">Обраний пацієнт</h3>
          <div className="grid grid-cols-3 gap-4 text-sm">
            <div><span className="text-gray-500 text-xs">ID:</span><div className="text-gray-800 font-mono">{patient.id}</div></div>
            <div><span className="text-gray-500 text-xs">Ім'я:</span><div className="text-gray-800">{patient.name}</div></div>
            <div><span className="text-gray-500 text-xs">Дата народження:</span><div className="text-gray-800">{patient.dob}</div></div>
          </div>
        </div>

        <div className="bg-white border border-gray-200 rounded">
          <div className="px-5 py-4 border-b border-gray-200">
            <h2 className="text-gray-900">Замовлення протезів</h2>
            <p className="text-gray-500 text-xs mt-0.5">Замовлення, отримані через API від Doctor Eleks</p>
          </div>

          {orders.length === 0 ? (
            <div className="py-10 text-center text-gray-400 text-sm">Немає замовлень для цього пацієнта</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  {["Номер замовлення", "Тип протезу", "Дата призначення", "Статус", "Дія"].map(col => (
                    <th key={col} className="text-left px-4 py-3 text-gray-600 font-medium text-xs">{col}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {orders.map(o => (
                  <tr key={o.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-xs text-gray-700">{o.orderNumber}</td>
                    <td className="px-4 py-3 text-gray-800">{o.prosthesisType}</td>
                    <td className="px-4 py-3 text-gray-600">{o.prescriptionDate}</td>
                    <td className="px-4 py-3">
                      <span className="px-2 py-0.5 rounded border text-xs bg-gray-100 text-gray-700 border-gray-300">{o.status}</span>
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => onSelect(o)}
                        className="text-xs bg-gray-800 text-white px-3 py-1.5 rounded hover:bg-gray-700"
                      >
                        Обрати
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
