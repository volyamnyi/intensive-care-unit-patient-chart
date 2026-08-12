import { ArrowLeft, FileText, Paperclip, Home } from "lucide-react";
import { Patient, Order } from "./mockData";

interface Props {
  patient: Patient;
  order: Order;
  onContinue: () => void;
  onBack: () => void;
  onMainMenu: () => void;
}

export function OrderReview({ patient, order, onContinue, onBack, onMainMenu }: Props) {
  return (
    <div className="flex-1 overflow-auto bg-gray-50 p-6">
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center gap-3 mb-6">
          <button onClick={onBack} className="flex items-center gap-1 text-gray-500 hover:text-gray-800 text-sm">
            <ArrowLeft size={16} /> Назад
          </button>
          <div className="text-gray-300">|</div>
          <div>
            <h1 className="text-gray-900">Перегляд замовлення</h1>
            <p className="text-gray-500 text-sm">Крок 3 з 4 — Ознайомтесь з документами замовлення</p>
          </div>
        </div>

        {/* Progress indicator */}
        <div className="flex items-center gap-0 mb-6">
          {["Пацієнт", "Замовлення", "Перегляд", "Шаблон"].map((step, i) => (
            <div key={i} className="flex items-center">
              <div className={`flex items-center gap-2 px-3 py-1.5 text-xs ${i === 2 ? "bg-gray-800 text-white rounded" : i < 2 ? "text-gray-600" : "text-gray-400"}`}>
                <span className={`w-5 h-5 rounded-full border flex items-center justify-center ${i === 2 ? "bg-white text-gray-800 border-white" : i < 2 ? "bg-gray-600 text-white border-gray-600" : "border-gray-300"}`}>{i < 2 ? "✓" : i + 1}</span>
                {step}
              </div>
              {i < 3 && <div className="w-8 h-px bg-gray-300" />}
            </div>
          ))}
        </div>

        <div className="grid grid-cols-[240px_1fr_200px] gap-4 h-[calc(100vh-260px)]">
          {/* Left panel */}
          <div className="space-y-3">
            <div className="bg-white border border-gray-200 rounded p-4">
              <h3 className="text-gray-700 text-xs font-medium uppercase mb-3 tracking-wider">Пацієнт</h3>
              <div className="space-y-2 text-sm">
                <div><span className="text-gray-500 text-xs block">ID</span><span className="text-gray-800 font-mono">{patient.id}</span></div>
                <div><span className="text-gray-500 text-xs block">Ім'я</span><span className="text-gray-800">{patient.name}</span></div>
                <div><span className="text-gray-500 text-xs block">Дата народження</span><span className="text-gray-800">{patient.dob}</span></div>
                <div><span className="text-gray-500 text-xs block">Статус</span><span className="text-gray-800">{patient.status}</span></div>
              </div>
            </div>
            <div className="bg-white border border-gray-200 rounded p-4">
              <h3 className="text-gray-700 text-xs font-medium uppercase mb-3 tracking-wider">Замовлення</h3>
              <div className="space-y-2 text-sm">
                <div><span className="text-gray-500 text-xs block">Номер</span><span className="text-gray-800 font-mono">{order.orderNumber}</span></div>
                <div><span className="text-gray-500 text-xs block">Тип протезу</span><span className="text-gray-800">{order.prosthesisType}</span></div>
                <div><span className="text-gray-500 text-xs block">Дата призначення</span><span className="text-gray-800">{order.prescriptionDate}</span></div>
                <div><span className="text-gray-500 text-xs block">Статус</span><span className="text-gray-800">{order.status}</span></div>
              </div>
            </div>
            <div className="bg-yellow-50 border border-yellow-200 rounded p-3 text-xs text-yellow-800">
              Дані отримані через API з Doctor Eleks
            </div>
          </div>

          {/* Center - document viewer */}
          <div className="bg-white border border-gray-200 rounded flex flex-col">
            <div className="px-4 py-3 border-b border-gray-200 flex items-center gap-2">
              <FileText size={16} className="text-gray-500" />
              <span className="text-sm text-gray-700">Медична документація — {order.orderNumber}.pdf</span>
            </div>
            <div className="flex-1 overflow-auto p-6 bg-gray-50">
              {/* Mock document */}
              <div className="bg-white border border-gray-200 shadow-sm rounded p-8 max-w-2xl mx-auto">
                <div className="text-center mb-6 border-b border-gray-200 pb-4">
                  <div className="w-16 h-16 bg-gray-200 rounded mx-auto mb-2 flex items-center justify-center text-xs text-gray-400">ЛОГО</div>
                  <h2 className="text-gray-800">НАПРАВЛЕННЯ НА ПРОТЕЗУВАННЯ</h2>
                  <p className="text-gray-500 text-sm">Doctor Eleks — Медична інформаційна система</p>
                </div>
                <div className="space-y-4 text-sm text-gray-700">
                  <div className="grid grid-cols-2 gap-4">
                    <div><strong>Пацієнт:</strong> {patient.name}</div>
                    <div><strong>Дата народження:</strong> {patient.dob}</div>
                    <div><strong>Номер замовлення:</strong> {order.orderNumber}</div>
                    <div><strong>Дата призначення:</strong> {order.prescriptionDate}</div>
                  </div>
                  <div className="border-t border-gray-200 pt-4">
                    <strong>Тип протезу:</strong> {order.prosthesisType}
                  </div>
                  <div>
                    <strong>Клінічний діагноз:</strong>
                    <p className="mt-1 text-gray-600 leading-relaxed">Ампутація на рівні гомілки (транстибіальна). Стан після травматичної ампутації. Куксоутворення завершено. Рекомендовано первинне протезування.</p>
                  </div>
                  <div>
                    <strong>Вимоги до протезу:</strong>
                    <ul className="mt-1 ml-4 space-y-1 text-gray-600 list-disc">
                      <li>Приймальна гільза: ТЕС або гідравлічна</li>
                      <li>Модульна конструкція</li>
                      <li>Динамічна стопа (SACH або еквівалент)</li>
                      <li>Покриття: косметичне</li>
                    </ul>
                  </div>
                  <div className="border-t border-gray-200 pt-4 grid grid-cols-2 gap-4">
                    <div>
                      <strong>Лікар:</strong>
                      <p className="text-gray-600">д-р Василенко О.П.</p>
                      <div className="mt-3 border-b border-gray-400 w-32" />
                      <p className="text-xs text-gray-400 mt-1">Підпис</p>
                    </div>
                    <div>
                      <strong>Дата:</strong>
                      <p className="text-gray-600">{order.prescriptionDate}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right panel - metadata and attachments */}
          <div className="space-y-3">
            <div className="bg-white border border-gray-200 rounded p-4">
              <h3 className="text-gray-700 text-xs font-medium uppercase mb-3 tracking-wider">Метадані</h3>
              <div className="space-y-2 text-xs text-gray-600">
                <div className="flex justify-between"><span className="text-gray-400">Тип</span><span>PDF</span></div>
                <div className="flex justify-between"><span className="text-gray-400">Розмір</span><span>284 KB</span></div>
                <div className="flex justify-between"><span className="text-gray-400">Сторінок</span><span>3</span></div>
                <div className="flex justify-between"><span className="text-gray-400">Джерело</span><span>Doctor Eleks</span></div>
                <div className="flex justify-between"><span className="text-gray-400">Отримано</span><span>{order.prescriptionDate}</span></div>
              </div>
            </div>
            <div className="bg-white border border-gray-200 rounded p-4">
              <h3 className="text-gray-700 text-xs font-medium uppercase mb-3 tracking-wider">Вкладення</h3>
              <div className="space-y-2">
                {["Рентген_2024.jpg", "Лабораторні_дані.pdf", "Мірки_кукси.pdf"].map(f => (
                  <div key={f} className="flex items-center gap-2 text-xs text-gray-600 hover:text-gray-800 cursor-pointer">
                    <Paperclip size={12} className="text-gray-400" />
                    <span>{f}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex justify-between mt-4">
          <div className="flex gap-2">
            <button onClick={onBack} className="px-4 py-2 border border-gray-300 rounded text-sm text-gray-700 hover:bg-gray-100">
              Назад
            </button>
            <button onClick={onMainMenu} className="flex items-center gap-1.5 px-4 py-2 border border-gray-300 rounded text-sm text-gray-700 hover:bg-gray-100">
              <Home size={14} /> Головна
            </button>
          </div>
          <button onClick={onContinue} className="px-6 py-2 bg-gray-800 text-white rounded text-sm hover:bg-gray-700">
            Продовжити →
          </button>
        </div>
      </div>
    </div>
  );
}
