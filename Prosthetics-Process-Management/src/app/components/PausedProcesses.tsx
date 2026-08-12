import { Play, Eye, ArrowLeft } from "lucide-react";
import { Process } from "./mockData";

interface Props {
  processes: Process[];
  onResume: (process: Process) => void;
  onViewDetails: (process: Process) => void;
  onBack: () => void;
}

export function PausedProcesses({ processes, onResume, onViewDetails, onBack }: Props) {
  const paused = processes.filter(p => p.status === "Paused");

  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={onBack} className="flex items-center gap-1 text-gray-500 hover:text-gray-800 text-sm">
            <ArrowLeft size={16} /> Назад
          </button>
          <div className="text-gray-300">|</div>
          <div>
            <h1 className="text-gray-900">Призупинені процеси</h1>
            <p className="text-gray-500 text-sm">{paused.length} процесів на паузі</p>
          </div>
        </div>

        <div className="bg-white border border-gray-200 rounded">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                {["ID процесу", "Пацієнт", "Замовлення", "Поточний етап", "Поточний крок", "Дата паузи", "Дії"].map(col => (
                  <th key={col} className="text-left px-4 py-3 text-gray-600 font-medium text-xs">{col}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {paused.length === 0 ? (
                <tr><td colSpan={7} className="py-10 text-center text-gray-400">Немає призупинених процесів</td></tr>
              ) : paused.map(p => (
                <tr key={p.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-xs text-gray-600">{p.id}</td>
                  <td className="px-4 py-3 text-gray-800">{p.patientName}</td>
                  <td className="px-4 py-3 text-gray-600 text-xs">{p.orderNumber}</td>
                  <td className="px-4 py-3 text-gray-600">{p.currentStage}</td>
                  <td className="px-4 py-3 text-gray-600">{p.currentStep}</td>
                  <td className="px-4 py-3 text-gray-500 text-xs">{p.pauseDate}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button onClick={() => onResume(p)} className="flex items-center gap-1 text-xs bg-gray-800 text-white px-3 py-1.5 rounded hover:bg-gray-700">
                        <Play size={12} /> Відновити
                      </button>
                      <button onClick={() => onViewDetails(p)} className="flex items-center gap-1 text-xs border border-gray-300 text-gray-600 px-3 py-1.5 rounded hover:bg-gray-50">
                        <Eye size={12} /> Деталі
                      </button>
                    </div>
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
