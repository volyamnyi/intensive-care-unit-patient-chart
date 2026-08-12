import { useState } from "react";
import { Search, ArrowLeft } from "lucide-react";
import { MOCK_PATIENTS, Patient } from "./mockData";

interface Props {
  onSelect: (patient: Patient) => void;
  onBack: () => void;
}

export function PatientSelection({ onSelect, onBack }: Props) {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");

  const filtered = MOCK_PATIENTS.filter(p => {
    const matchSearch = p.name.toLowerCase().includes(search.toLowerCase()) || p.id.toLowerCase().includes(search.toLowerCase());
    const matchStatus = statusFilter === "all" || p.status === statusFilter;
    return matchSearch && matchStatus;
  });

  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={onBack} className="flex items-center gap-1 text-gray-500 hover:text-gray-800 text-sm">
            <ArrowLeft size={16} /> Назад
          </button>
          <div className="text-gray-300">|</div>
          <div>
            <h1 className="text-gray-900">Вибір пацієнта</h1>
            <p className="text-gray-500 text-sm">Крок 1 з 4 — Знайдіть та оберіть пацієнта</p>
          </div>
        </div>

        {/* Progress indicator */}
        <div className="flex items-center gap-0 mb-6">
          {["Пацієнт", "Замовлення", "Перегляд", "Шаблон"].map((step, i) => (
            <div key={i} className="flex items-center">
              <div className={`flex items-center gap-2 px-3 py-1.5 text-xs ${i === 0 ? "bg-gray-800 text-white rounded" : "text-gray-400"}`}>
                <span className={`w-5 h-5 rounded-full border flex items-center justify-center ${i === 0 ? "bg-white text-gray-800 border-white" : "border-gray-300"}`}>{i + 1}</span>
                {step}
              </div>
              {i < 3 && <div className="w-8 h-px bg-gray-300" />}
            </div>
          ))}
        </div>

        <div className="bg-white border border-gray-200 rounded">
          <div className="p-4 border-b border-gray-200 flex gap-3">
            <div className="flex-1 relative">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Пошук за ім'ям або ID пацієнта..."
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded text-sm focus:outline-none focus:border-gray-500"
              />
            </div>
            <select
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
              className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none"
            >
              <option value="all">Всі статуси</option>
              <option value="Active">Активні</option>
              <option value="Inactive">Неактивні</option>
            </select>
          </div>

          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                {["ID пацієнта", "Повне ім'я", "Дата народження", "Статус", "Дія"].map(col => (
                  <th key={col} className="text-left px-4 py-3 text-gray-600 font-medium text-xs">{col}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filtered.map(p => (
                <tr key={p.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-xs text-gray-600">{p.id}</td>
                  <td className="px-4 py-3 text-gray-900">{p.name}</td>
                  <td className="px-4 py-3 text-gray-600">{p.dob}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded border text-xs ${p.status === "Active" ? "bg-gray-100 text-gray-700 border-gray-300" : "bg-gray-50 text-gray-400 border-gray-200"}`}>{p.status}</span>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => onSelect(p)}
                      className="text-xs bg-gray-800 text-white px-3 py-1.5 rounded hover:bg-gray-700"
                    >
                      Обрати
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
