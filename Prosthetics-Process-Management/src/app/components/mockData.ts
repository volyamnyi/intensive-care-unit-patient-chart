export type ProcessStatus =
  | "New"
  | "In Progress"
  | "Waiting for Review"
  | "Paused"
  | "Failed Quality Check"
  | "Failed"
  | "Completed";

export interface Patient {
  id: string;
  name: string;
  dob: string;
  status: string;
}

export interface Order {
  id: string;
  patientId: string;
  orderNumber: string;
  prosthesisType: string;
  prescriptionDate: string;
  status: string;
}

export interface ProcessTemplate {
  id: string;
  name: string;
  description: string;
  estimatedDuration: string;
  stageCount: number;
  stages: Stage[];
}

export interface Stage {
  id: string;
  name: string;
  type: "clinical" | "production" | "quality";
  steps: Step[];
}

export interface Step {
  id: string;
  name: string;
  instruction: string;
  controls: Control[];
}

export interface Control {
  type: "checkbox" | "text" | "number" | "dropdown" | "radio" | "textarea" | "date" | "file" | "image" | "signature";
  label: string;
  options?: string[];
  required?: boolean;
}

export interface Process {
  id: string;
  patientId: string;
  patientName: string;
  orderId: string;
  orderNumber: string;
  templateId: string;
  templateName: string;
  currentStage: string;
  currentStep: string;
  status: ProcessStatus;
  lastUpdated: string;
  assignedUser: string;
  startDate: string;
  pauseDate?: string;
  history: HistoryEvent[];
  stageIndex: number;
  stepIndex: number;
}

export interface HistoryEvent {
  id: string;
  type: string;
  description: string;
  user: string;
  timestamp: string;
}

export const MOCK_PATIENTS: Patient[] = [
  { id: "P001", name: "Іваненко Олексій Петрович", dob: "1975-03-15", status: "Active" },
  { id: "P002", name: "Коваль Марія Сергіївна", dob: "1982-07-22", status: "Active" },
  { id: "P003", name: "Мельник Василь Андрійович", dob: "1968-11-08", status: "Active" },
  { id: "P004", name: "Петренко Надія Іванівна", dob: "1990-04-30", status: "Inactive" },
  { id: "P005", name: "Сидоренко Дмитро Олегович", dob: "1955-09-12", status: "Active" },
];

export const MOCK_ORDERS: Order[] = [
  { id: "O001", patientId: "P001", orderNumber: "PRO-2024-001", prosthesisType: "Протез нижньої кінцівки (транстибіальний)", prescriptionDate: "2024-01-15", status: "Pending" },
  { id: "O002", patientId: "P001", orderNumber: "PRO-2024-008", prosthesisType: "Ортез коліна", prescriptionDate: "2024-02-20", status: "In Progress" },
  { id: "O003", patientId: "P002", orderNumber: "PRO-2024-003", prosthesisType: "Протез верхньої кінцівки (трансрадіальний)", prescriptionDate: "2024-01-28", status: "Pending" },
  { id: "O004", patientId: "P003", orderNumber: "PRO-2024-005", prosthesisType: "Протез нижньої кінцівки (трансфеморальний)", prescriptionDate: "2024-02-05", status: "Pending" },
  { id: "O005", patientId: "P005", orderNumber: "PRO-2024-012", prosthesisType: "Косметичний протез кисті", prescriptionDate: "2024-03-01", status: "Pending" },
];

export const MOCK_TEMPLATES: ProcessTemplate[] = [
  {
    id: "T001",
    name: "Протез нижньої кінцівки",
    description: "Повний цикл виготовлення протезу нижньої кінцівки — від зняття мірок до фінальної підгонки",
    estimatedDuration: "14-21 день",
    stageCount: 5,
    stages: [
      {
        id: "S1", name: "Клінічна оцінка", type: "clinical",
        steps: [
          { id: "S1-1", name: "Первинний огляд", instruction: "Проведіть первинний огляд пацієнта та задокументуйте стан кукси.", controls: [{ type: "textarea", label: "Опис стану кукси", required: true }, { type: "image", label: "Фото кукси" }] },
          { id: "S1-2", name: "Зняття мірок", instruction: "Зніміть точні мірки відповідно до протоколу.", controls: [{ type: "number", label: "Довжина кукси (мм)", required: true }, { type: "number", label: "Окружність проксимальна (мм)", required: true }, { type: "number", label: "Окружність дистальна (мм)", required: true }] },
        ]
      },
      {
        id: "S2", name: "Виготовлення гільзи", type: "production",
        steps: [
          { id: "S2-1", name: "Підготовка матеріалів", instruction: "Підготуйте всі необхідні матеріали для виготовлення гільзи.", controls: [{ type: "checkbox", label: "Ламінат підготовлено", required: true }, { type: "checkbox", label: "Смола підготовлена", required: true }, { type: "dropdown", label: "Тип матеріалу", options: ["Вуглеволокно", "Скловолокно", "Гібридний"], required: true }] },
          { id: "S2-2", name: "Формування гільзи", instruction: "Сформуйте гільзу згідно з технологічною картою.", controls: [{ type: "number", label: "Температура формування (°C)" }, { type: "number", label: "Час витримки (хв)" }, { type: "textarea", label: "Примітки" }] },
          { id: "S2-3", name: "Обробка та підгонка", instruction: "Проведіть механічну обробку та первинну підгонку гільзи.", controls: [{ type: "checkbox", label: "Обрізання виконано" }, { type: "checkbox", label: "Краї оброблено" }] },
        ]
      },
      {
        id: "S3", name: "Контроль якості гільзи", type: "quality",
        steps: [
          { id: "S3-1", name: "Перевірка якості", instruction: "Перевірте відповідність гільзи технічним вимогам.", controls: [{ type: "checkbox", label: "Розміри відповідають", required: true }, { type: "checkbox", label: "Відсутність дефектів поверхні", required: true }, { type: "textarea", label: "Результати перевірки" }] },
        ]
      },
      {
        id: "S4", name: "Збірка та монтаж", type: "production",
        steps: [
          { id: "S4-1", name: "Монтаж компонентів", instruction: "Встановіть кінематичні компоненти та з'єднайте всі елементи протезу.", controls: [{ type: "dropdown", label: "Тип стопи", options: ["SACH", "Dynamic", "Multi-axial"] }, { type: "checkbox", label: "Адаптери встановлено" }, { type: "number", label: "Момент затяжки болтів (Нм)" }] },
          { id: "S4-2", name: "Вирівнювання", instruction: "Проведіть статичне вирівнювання протезу.", controls: [{ type: "checkbox", label: "Статичне вирівнювання виконано" }, { type: "image", label: "Фото вирівнювання" }] },
        ]
      },
      {
        id: "S5", name: "Фінальне підгонка та передача", type: "clinical",
        steps: [
          { id: "S5-1", name: "Динамічне вирівнювання", instruction: "Проведіть динамічне вирівнювання з пацієнтом.", controls: [{ type: "checkbox", label: "Динамічне вирівнювання завершено" }, { type: "textarea", label: "Спостереження" }] },
          { id: "S5-2", name: "Навчання пацієнта", instruction: "Проведіть інструктаж пацієнта з використання та догляду.", controls: [{ type: "checkbox", label: "Інструктаж проведено" }, { type: "signature", label: "Підпис пацієнта", required: true }] },
        ]
      },
    ]
  },
  {
    id: "T002",
    name: "Протез верхньої кінцівки",
    description: "Процес виготовлення функціонального або косметичного протезу верхньої кінцівки",
    estimatedDuration: "10-18 день",
    stageCount: 4,
    stages: []
  },
  {
    id: "T003",
    name: "Виготовлення гільзи (Socket Fabrication)",
    description: "Спеціалізований процес виготовлення приймальної гільзи для існуючого протезу",
    estimatedDuration: "5-7 день",
    stageCount: 3,
    stages: []
  },
  {
    id: "T004",
    name: "Повторне підгонка (Refitting)",
    description: "Процес повторної підгонки та корекції існуючого протезу",
    estimatedDuration: "2-5 день",
    stageCount: 2,
    stages: []
  },
  {
    id: "T005",
    name: "Ремонт протезу",
    description: "Діагностика та ремонт пошкодженого протезу",
    estimatedDuration: "1-3 день",
    stageCount: 2,
    stages: []
  },
];

export const MOCK_PROCESSES: Process[] = [
  {
    id: "WO-2024-001", patientId: "P001", patientName: "Іваненко Олексій Петрович",
    orderId: "O001", orderNumber: "PRO-2024-001",
    templateId: "T001", templateName: "Протез нижньої кінцівки",
    currentStage: "Виготовлення гільзи", currentStep: "Формування гільзи",
    status: "In Progress", lastUpdated: "2024-03-15 14:30",
    assignedUser: "Коваленко М.В.", startDate: "2024-03-10",
    stageIndex: 1, stepIndex: 1,
    history: [
      { id: "H1", type: "Process Started", description: "Процес розпочато", user: "Коваленко М.В.", timestamp: "2024-03-10 09:00" },
      { id: "H2", type: "Stage Started", description: "Розпочато етап: Клінічна оцінка", user: "Коваленко М.В.", timestamp: "2024-03-10 09:05" },
      { id: "H3", type: "Stage Completed", description: "Завершено етап: Клінічна оцінка", user: "Коваленко М.В.", timestamp: "2024-03-11 16:00" },
      { id: "H4", type: "Stage Started", description: "Розпочато етап: Виготовлення гільзи", user: "Коваленко М.В.", timestamp: "2024-03-12 08:30" },
    ]
  },
  {
    id: "WO-2024-002", patientId: "P002", patientName: "Коваль Марія Сергіївна",
    orderId: "O003", orderNumber: "PRO-2024-003",
    templateId: "T002", templateName: "Протез верхньої кінцівки",
    currentStage: "Клінічна оцінка", currentStep: "Первинний огляд",
    status: "New", lastUpdated: "2024-03-14 10:00",
    assignedUser: "Коваленко М.В.", startDate: "2024-03-14",
    stageIndex: 0, stepIndex: 0,
    history: []
  },
  {
    id: "WO-2024-003", patientId: "P003", patientName: "Мельник Василь Андрійович",
    orderId: "O004", orderNumber: "PRO-2024-005",
    templateId: "T001", templateName: "Протез нижньої кінцівки",
    currentStage: "Контроль якості гільзи", currentStep: "Перевірка якості",
    status: "Paused", lastUpdated: "2024-03-13 17:00",
    assignedUser: "Коваленко М.В.", startDate: "2024-03-05",
    pauseDate: "2024-03-13 17:00",
    stageIndex: 2, stepIndex: 0,
    history: []
  },
  {
    id: "WO-2024-004", patientId: "P001", patientName: "Іваненко Олексій Петрович",
    orderId: "O002", orderNumber: "PRO-2024-008",
    templateId: "T004", templateName: "Повторне підгонка",
    currentStage: "Фінальна підгонка", currentStep: "Фінальна підгонка",
    status: "Completed", lastUpdated: "2024-03-12 16:00",
    assignedUser: "Коваленко М.В.", startDate: "2024-03-08",
    stageIndex: 1, stepIndex: 0,
    history: []
  },
  {
    id: "WO-2024-005", patientId: "P005", patientName: "Сидоренко Дмитро Олегович",
    orderId: "O005", orderNumber: "PRO-2024-012",
    templateId: "T003", templateName: "Виготовлення гільзи",
    currentStage: "Виготовлення", currentStep: "Формування",
    status: "Failed", lastUpdated: "2024-03-10 11:00",
    assignedUser: "Коваленко М.В.", startDate: "2024-03-07",
    stageIndex: 1, stepIndex: 0,
    history: []
  },
];
