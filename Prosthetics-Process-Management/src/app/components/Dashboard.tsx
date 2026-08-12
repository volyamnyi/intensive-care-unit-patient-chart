import { Plus, Play, PauseCircle, CheckCircle, XCircle, History } from "lucide-react";
import { MOCK_PROCESSES, Process, ProcessStatus } from "./mockData";

interface Props {
  user: string;
  onNewProcess: () => void;
  onOpenProcess: (process: Process) => void;
  onPausedProcesses: () => void;
  processes: Process[];
}

const STATUS_COLORS: Record<ProcessStatus, string> = {
  "New": "bg-gray-100 text-gray-700 border-gray-300",
  "In Progress": "bg-gray-200 text-gray-800 border-gray-400",
  "Waiting for Review": "bg-gray-300 text-gray-800 border-gray-500",
  "Paused": "bg-gray-100 text-gray-600 border-gray-300",
  "Failed Quality Check": "bg-gray-200 text-gray-700 border-gray-400",
  "Failed": "bg-gray-700 text-white border-gray-700",
  "Completed": "bg-gray-900 text-white border-gray-900",
};

interface FilterTab {
  label: string;
  filter: ProcessStatus | "all";
  icon: React.ReactNode;
  count: number;
}

export function Dashboard({ user, onNewProcess, onOpenProcess, onPausedProcesses, processes }: Props) {
  const counts = {
    active: processes.filter(p => p.status === "In Progress" || p.status === "Waiting for Review").length,
    paused: processes.filter(p => p.status === "Paused").length,
    completed: processes.filter(p => p.status === "Completed").length,
    failed: processes.filter(p => p.status === "Failed" || p.status === "Failed Quality Check").length,
  };

  return (
    <div className="flex-1 overflow-auto bg-gray-50">
      <div className="p-6">
        {/* Summary cards */}
        <div className="grid grid-cols-4 gap-4 mb-6">
          <div className="bg-white border border-gray-200 rounded p-4">
            <div className="flex items-center gap-2 mb-2 text-gray-500">
              <Play size={16} />
              <span className="text-sm">Активні</span>
            </div>
            <div className="text-2xl font-semibold text-gray-900">{counts.active}</div>
          </div>
          <div className="bg-white border border-gray-200 rounded p-4 cursor-pointer hover:border-gray-400" onClick={onPausedProcesses}>
            <div className="flex items-center gap-2 mb-2 text-gray-500">
              <PauseCircle size={16} />
              <span className="text-sm">Призупинені</span>
            </div>
            <div className="text-2xl font-semibold text-gray-900">{counts.paused}</div>
          </div>
          <div className="bg-white border border-gray-200 rounded p-4">
            <div className="flex items-center gap-2 mb-2 text-gray-500">
              <CheckCircle size={16} />
              <span className="text-sm">Завершені</span>
            </div>
            <div className="text-2xl font-semibold text-gray-900">{counts.completed}</div>
          </div>
          <div className="bg-white border border-gray-200 rounded p-4">
            <div className="flex items-center gap-2 mb-2 text-gray-500">
              <XCircle size={16} />
              <span className="text-sm">Провалені</span>
            </div>
            <div className="text-2xl font-semibold text-gray-900">{counts.failed}</div>
          </div>
        </div>

        {/* Main table */}
        <div className="bg-white border border-gray-200 rounded">
          <div className="flex items-center justify-between px-5 py-4 border-b border-gray-200">
            <h2 className="text-gray-900">Мої технологічні процеси</h2>
            <button
              onClick={onNewProcess}
              className="flex items-center gap-2 bg-gray-800 text-white px-4 py-2 rounded text-sm hover:bg-gray-700 transition-colors"
            >
              <Plus size={16} />
              Новий процес
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  {["ID процесу", "Пацієнт", "Замовлення", "Шаблон процесу", "Поточний етап", "Поточний крок", "Статус", "Оновлено"].map(col => (
                    <th key={col} className="text-left px-4 py-3 text-gray-600 font-medium text-xs">{col}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {processes.map(p => (
                  <tr
                    key={p.id}
                    onClick={() => onOpenProcess(p)}
                    className="hover:bg-gray-50 cursor-pointer"
                  >
                    <td className="px-4 py-3 text-gray-700 font-mono text-xs">{p.id}</td>
                    <td className="px-4 py-3 text-gray-800">{p.patientName}</td>
                    <td className="px-4 py-3 text-gray-600 text-xs">{p.orderNumber}</td>
                    <td className="px-4 py-3 text-gray-700">{p.templateName}</td>
                    <td className="px-4 py-3 text-gray-600">{p.currentStage}</td>
                    <td className="px-4 py-3 text-gray-600">{p.currentStep}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded border text-xs ${STATUS_COLORS[p.status]}`}>{p.status}</span>
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{p.lastUpdated}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {processes.length === 0 && (
              <div className="py-12 text-center text-gray-400 text-sm">Немає процесів. Створіть новий процес.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
