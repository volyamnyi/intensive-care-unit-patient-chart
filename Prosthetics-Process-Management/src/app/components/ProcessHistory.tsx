import { ArrowLeft, Clock, CheckCircle, XCircle, RotateCcw, PauseCircle, Play, AlertTriangle } from "lucide-react";
import { Process } from "./mockData";

interface Props {
  process: Process;
  onBack: () => void;
}

const EVENT_ICONS: Record<string, React.ReactNode> = {
  "Process Started": <Play size={14} />,
  "Stage Started": <Play size={14} />,
  "Stage Completed": <CheckCircle size={14} />,
  "Quality Check Passed": <CheckCircle size={14} />,
  "Quality Check Failed": <XCircle size={14} />,
  "Rework Started": <RotateCcw size={14} />,
  "Rework Completed": <CheckCircle size={14} />,
  "Pause": <PauseCircle size={14} />,
  "Resume": <Play size={14} />,
  "Process Failed": <XCircle size={14} />,
  "Process Completed": <CheckCircle size={14} />,
};

const allEvents = [
  { id: "E1", type: "Process Started", description: "Процес WO-2024-001 розпочато", user: "Коваленко М.В.", timestamp: "2024-03-10 09:00" },
  { id: "E2", type: "Stage Started", description: "Розпочато клінічний етап: Клінічна оцінка", user: "Коваленко М.В.", timestamp: "2024-03-10 09:05" },
  { id: "E3", type: "Stage Completed", description: "Завершено крок: Первинний огляд", user: "Коваленко М.В.", timestamp: "2024-03-10 11:30" },
  { id: "E4", type: "Stage Completed", description: "Завершено крок: Зняття мірок", user: "Коваленко М.В.", timestamp: "2024-03-11 09:00" },
  { id: "E5", type: "Stage Completed", description: "Завершено клінічний етап: Клінічна оцінка", user: "Коваленко М.В.", timestamp: "2024-03-11 16:00" },
  { id: "E6", type: "Stage Started", description: "Розпочато виробничий етап: Виготовлення гільзи", user: "Коваленко М.В.", timestamp: "2024-03-12 08:30" },
  { id: "E7", type: "Stage Completed", description: "Завершено крок: Підготовка матеріалів", user: "Коваленко М.В.", timestamp: "2024-03-12 10:00" },
  { id: "E8", type: "Pause", description: "Процес призупинено", user: "Коваленко М.В.", timestamp: "2024-03-12 17:00" },
  { id: "E9", type: "Resume", description: "Процес відновлено", user: "Коваленко М.В.", timestamp: "2024-03-13 08:30" },
];

export function ProcessHistory({ process, onBack }: Props) {
  const eventTypes = ["Всі", "Stage Completed", "Quality Check Passed", "Quality Check Failed", "Pause", "Resume", "Process Failed", "Process Completed"];

  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={onBack} className="flex items-center gap-1 text-gray-500 hover:text-gray-800 text-sm">
            <ArrowLeft size={16} /> Назад
          </button>
          <div className="text-gray-300">|</div>
          <div>
            <h1 className="text-gray-900">Аудит-лог процесу</h1>
            <p className="text-gray-500 text-sm">{process.id} — {process.patientName}</p>
          </div>
        </div>

        {/* Filters */}
        <div className="bg-white border border-gray-200 rounded p-4 mb-4 flex gap-3 flex-wrap">
          <select className="border border-gray-300 rounded px-3 py-1.5 text-sm focus:outline-none">
            <option>Всі користувачі</option>
            <option>Коваленко М.В.</option>
          </select>
          <input type="date" className="border border-gray-300 rounded px-3 py-1.5 text-sm focus:outline-none" />
          <input type="date" className="border border-gray-300 rounded px-3 py-1.5 text-sm focus:outline-none" />
          <select className="border border-gray-300 rounded px-3 py-1.5 text-sm focus:outline-none">
            {eventTypes.map(t => <option key={t}>{t}</option>)}
          </select>
        </div>

        {/* Timeline */}
        <div className="bg-white border border-gray-200 rounded p-6">
          <h3 className="text-gray-700 text-sm mb-5">Хронологія подій</h3>
          <div className="relative">
            <div className="absolute left-5 top-0 bottom-0 w-px bg-gray-200" />
            <div className="space-y-4">
              {allEvents.map(event => (
                <div key={event.id} className="flex gap-4 pl-2">
                  <div className={`w-7 h-7 rounded-full border-2 flex items-center justify-center shrink-0 z-10 ${
                    event.type.includes("Failed") ? "bg-gray-700 text-white border-gray-700" :
                    event.type.includes("Completed") || event.type.includes("Passed") ? "bg-gray-800 text-white border-gray-800" :
                    event.type === "Pause" ? "bg-gray-400 text-white border-gray-400" :
                    "bg-white text-gray-600 border-gray-300"
                  }`}>
                    {EVENT_ICONS[event.type] || <Clock size={12} />}
                  </div>
                  <div className="flex-1 pb-4 border-b border-gray-100 last:border-0">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-800">{event.description}</span>
                      <span className="text-xs text-gray-400 ml-4 shrink-0">{event.timestamp}</span>
                    </div>
                    <div className="text-xs text-gray-500 mt-0.5">
                      <span className="bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded">{event.type}</span>
                      <span className="ml-2">{event.user}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
