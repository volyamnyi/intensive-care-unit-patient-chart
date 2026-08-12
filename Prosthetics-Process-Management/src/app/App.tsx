import { useState } from "react";
import { LoginScreen } from "./components/LoginScreen";
import { Header } from "./components/Header";
import { Dashboard } from "./components/Dashboard";
import { PatientSelection } from "./components/PatientSelection";
import { OrderSelection } from "./components/OrderSelection";
import { OrderReview } from "./components/OrderReview";
import { TemplateSelection } from "./components/TemplateSelection";
import { ProcessOverview } from "./components/ProcessOverview";
import { ProcessExecution } from "./components/ProcessExecution";
import { QualityCheckpoint } from "./components/QualityCheckpoint";
import { ReworkStage } from "./components/ReworkStage";
import { PausedProcesses } from "./components/PausedProcesses";
import { ProcessHistory } from "./components/ProcessHistory";
import { FailedProcess } from "./components/FailedProcess";
import { FailureSnapshot } from "./components/FailureSnapshot";
import { CompletedProcess } from "./components/CompletedProcess";
import {
  Patient, Order, ProcessTemplate, Process, MOCK_PROCESSES, MOCK_TEMPLATES
} from "./components/mockData";

type Screen =
  | "login"
  | "dashboard"
  | "patient-selection"
  | "order-selection"
  | "order-review"
  | "template-selection"
  | "process-overview"
  | "process-execution"
  | "quality-checkpoint"
  | "rework"
  | "paused-processes"
  | "process-history"
  | "failed-process"
  | "failure-snapshot"
  | "completed-process";

export default function App() {
  const [screen, setScreen] = useState<Screen>("login");
  const [user, setUser] = useState("");
  const [processes, setProcesses] = useState<Process[]>(MOCK_PROCESSES);

  // New process wizard state
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [selectedTemplate, setSelectedTemplate] = useState<ProcessTemplate | null>(null);
  const [activeProcess, setActiveProcess] = useState<Process | null>(null);
  const [currentStageIndex, setCurrentStageIndex] = useState(0);
  const [currentStepIndex, setCurrentStepIndex] = useState(0);

  // Quality checkpoint stage tracker
  const [qualityStageIndex, setQualityStageIndex] = useState(0);

  const handleLogin = (u: string) => {
    setUser(u);
    setScreen("dashboard");
  };

  const handleLogout = () => {
    setUser("");
    setScreen("login");
  };

  const handleNewProcess = () => {
    setSelectedPatient(null);
    setSelectedOrder(null);
    setSelectedTemplate(null);
    setScreen("patient-selection");
  };

  const handlePatientSelect = (p: Patient) => {
    setSelectedPatient(p);
    setScreen("order-selection");
  };

  const handleOrderSelect = (o: Order) => {
    setSelectedOrder(o);
    setScreen("order-review");
  };

  const handleOrderReviewContinue = () => {
    setScreen("template-selection");
  };

  const handleTemplateSelect = (t: ProcessTemplate) => {
    setSelectedTemplate(t);
    // Create a new process
    const newProcess: Process = {
      id: `WO-${Date.now().toString().slice(-6)}`,
      patientId: selectedPatient!.id,
      patientName: selectedPatient!.name,
      orderId: selectedOrder!.id,
      orderNumber: selectedOrder!.orderNumber,
      templateId: t.id,
      templateName: t.name,
      currentStage: t.stages[0]?.name || "",
      currentStep: t.stages[0]?.steps[0]?.name || "",
      status: "New",
      lastUpdated: new Date().toLocaleString("uk-UA"),
      assignedUser: user,
      startDate: new Date().toISOString().split("T")[0],
      stageIndex: 0,
      stepIndex: 0,
      history: [],
    };
    setActiveProcess(newProcess);
    setCurrentStageIndex(0);
    setCurrentStepIndex(0);
    setScreen("process-overview");
  };

  const handleStartProcess = () => {
    if (activeProcess) {
      const updated = { ...activeProcess, status: "In Progress" as const, lastUpdated: new Date().toLocaleString("uk-UA") };
      setActiveProcess(updated);
      setProcesses(prev => [updated, ...prev]);
    }
    setScreen("process-execution");
  };

  const handleOpenProcess = (p: Process) => {
    setActiveProcess(p);
    const template = MOCK_TEMPLATES.find(t => t.id === p.templateId) || MOCK_TEMPLATES[0];
    setSelectedTemplate(template);
    setCurrentStageIndex(p.stageIndex);
    setCurrentStepIndex(p.stepIndex);
    if (p.status === "Failed") {
      setScreen("failed-process");
    } else if (p.status === "Completed") {
      setScreen("completed-process");
    } else {
      setScreen("process-execution");
    }
  };

  const handleUpdateProgress = (si: number, sti: number) => {
    setCurrentStageIndex(si);
    setCurrentStepIndex(sti);
    if (activeProcess && selectedTemplate) {
      const stage = selectedTemplate.stages[si];
      const step = stage?.steps[sti];
      const updated = {
        ...activeProcess,
        currentStage: stage?.name || activeProcess.currentStage,
        currentStep: step?.name || activeProcess.currentStep,
        stageIndex: si,
        stepIndex: sti,
        lastUpdated: new Date().toLocaleString("uk-UA"),
      };
      setActiveProcess(updated);
      setProcesses(prev => prev.map(p => p.id === updated.id ? updated : p));
    }
  };

  const handleQualityCheckpoint = () => {
    setQualityStageIndex(currentStageIndex + 1);
    setScreen("quality-checkpoint");
  };

  const handleQualityPass = () => {
    // Move past quality gate
    if (selectedTemplate) {
      const nextStageIndex = qualityStageIndex + 1;
      if (nextStageIndex >= selectedTemplate.stages.length) {
        handleProcessComplete();
      } else {
        setCurrentStageIndex(nextStageIndex);
        setCurrentStepIndex(0);
        setScreen("process-execution");
      }
    }
  };

  const handleRework = () => {
    setScreen("rework");
  };

  const handleStartRework = () => {
    // Route back to earlier production stage
    const prevStageIndex = Math.max(0, qualityStageIndex - 1);
    setCurrentStageIndex(prevStageIndex);
    setCurrentStepIndex(0);
    setScreen("process-execution");
  };

  const handleProcessFail = () => {
    if (activeProcess) {
      const failed = { ...activeProcess, status: "Failed" as const, lastUpdated: new Date().toLocaleString("uk-UA") };
      setActiveProcess(failed);
      setProcesses(prev => prev.map(p => p.id === failed.id ? failed : p));
    }
    setScreen("failed-process");
  };

  const handleProcessComplete = () => {
    if (activeProcess) {
      const completed = { ...activeProcess, status: "Completed" as const, lastUpdated: new Date().toLocaleString("uk-UA") };
      setActiveProcess(completed);
      setProcesses(prev => prev.map(p => p.id === completed.id ? completed : p));
    }
    setScreen("completed-process");
  };

  const handlePause = () => {
    if (activeProcess) {
      const paused = { ...activeProcess, status: "Paused" as const, pauseDate: new Date().toLocaleString("uk-UA"), lastUpdated: new Date().toLocaleString("uk-UA") };
      setActiveProcess(paused);
      setProcesses(prev => prev.map(p => p.id === paused.id ? paused : p));
    }
    setScreen("dashboard");
  };

  const handleResumeProcess = (p: Process) => {
    setActiveProcess(p);
    const template = MOCK_TEMPLATES.find(t => t.id === p.templateId) || MOCK_TEMPLATES[0];
    setSelectedTemplate(template);
    setCurrentStageIndex(p.stageIndex);
    setCurrentStepIndex(p.stepIndex);
    const resumed = { ...p, status: "In Progress" as const };
    setProcesses(prev => prev.map(proc => proc.id === resumed.id ? resumed : proc));
    setActiveProcess(resumed);
    setScreen("process-execution");
  };

  const breadcrumbMap: Partial<Record<Screen, string[]>> = {
    "dashboard": ["Дашборд"],
    "patient-selection": ["Дашборд", "Новий процес", "Вибір пацієнта"],
    "order-selection": ["Дашборд", "Новий процес", "Вибір замовлення"],
    "order-review": ["Дашборд", "Новий процес", "Перегляд замовлення"],
    "template-selection": ["Дашборд", "Новий процес", "Вибір шаблону"],
    "process-overview": ["Дашборд", "Огляд процесу"],
    "process-execution": ["Дашборд", activeProcess?.id || "Процес", "Виконання"],
    "quality-checkpoint": ["Дашборд", activeProcess?.id || "Процес", "Контроль якості"],
    "rework": ["Дашборд", activeProcess?.id || "Процес", "Доопрацювання"],
    "paused-processes": ["Дашборд", "Призупинені процеси"],
    "failed-process": ["Дашборд", "Провалений процес"],
    "failure-snapshot": ["Дашборд", "Провалений процес", "Знімок стану"],
    "completed-process": ["Дашборд", "Завершений процес"],
  };

  if (screen === "login") {
    return <LoginScreen onLogin={handleLogin} />;
  }

  return (
    <div className="h-screen flex flex-col bg-gray-50 overflow-hidden">
      <Header user={user} onLogout={handleLogout} breadcrumb={breadcrumbMap[screen]} />

      {screen === "dashboard" && (
        <Dashboard
          user={user}
          processes={processes}
          onNewProcess={handleNewProcess}
          onOpenProcess={handleOpenProcess}
          onPausedProcesses={() => setScreen("paused-processes")}
        />
      )}

      {screen === "patient-selection" && (
        <PatientSelection
          onSelect={handlePatientSelect}
          onBack={() => setScreen("dashboard")}
        />
      )}

      {screen === "order-selection" && selectedPatient && (
        <OrderSelection
          patient={selectedPatient}
          onSelect={handleOrderSelect}
          onBack={() => setScreen("patient-selection")}
        />
      )}

      {screen === "order-review" && selectedPatient && selectedOrder && (
        <OrderReview
          patient={selectedPatient}
          order={selectedOrder}
          onContinue={handleOrderReviewContinue}
          onBack={() => setScreen("order-selection")}
          onMainMenu={() => setScreen("dashboard")}
        />
      )}

      {screen === "template-selection" && (
        <TemplateSelection
          onSelect={handleTemplateSelect}
          onBack={() => setScreen("order-review")}
        />
      )}

      {screen === "process-overview" && activeProcess && selectedTemplate && selectedPatient && selectedOrder && (
        <ProcessOverview
          process={activeProcess}
          template={selectedTemplate}
          patient={selectedPatient}
          order={selectedOrder}
          onStart={handleStartProcess}
          onBack={() => setScreen("template-selection")}
        />
      )}

      {screen === "process-execution" && activeProcess && selectedTemplate && (
        <ProcessExecution
          process={activeProcess}
          template={selectedTemplate}
          onComplete={handleProcessComplete}
          onQualityCheck={handleQualityCheckpoint}
          onPause={handlePause}
          onMainMenu={() => setScreen("dashboard")}
          stageIndex={currentStageIndex}
          stepIndex={currentStepIndex}
          onUpdateProgress={handleUpdateProgress}
        />
      )}

      {screen === "quality-checkpoint" && activeProcess && selectedTemplate && (
        <QualityCheckpoint
          process={activeProcess}
          template={selectedTemplate}
          stageIndex={qualityStageIndex}
          onPass={handleQualityPass}
          onRework={handleRework}
          onFail={handleProcessFail}
        />
      )}

      {screen === "rework" && activeProcess && selectedTemplate && (
        <ReworkStage
          process={activeProcess}
          template={selectedTemplate}
          onStartRework={handleStartRework}
          onMainMenu={() => setScreen("dashboard")}
        />
      )}

      {screen === "paused-processes" && (
        <PausedProcesses
          processes={processes}
          onResume={handleResumeProcess}
          onViewDetails={(p) => { setActiveProcess(p); setScreen("process-history"); }}
          onBack={() => setScreen("dashboard")}
        />
      )}

      {screen === "process-history" && activeProcess && (
        <ProcessHistory
          process={activeProcess}
          onBack={() => setScreen("dashboard")}
        />
      )}

      {screen === "failed-process" && activeProcess && (
        <FailedProcess
          process={activeProcess}
          onViewSnapshot={() => setScreen("failure-snapshot")}
          onCreateReplacement={handleNewProcess}
          onBack={() => setScreen("dashboard")}
        />
      )}

      {screen === "failure-snapshot" && activeProcess && (
        <FailureSnapshot
          process={activeProcess}
          onBack={() => setScreen("failed-process")}
          onMainMenu={() => setScreen("dashboard")}
        />
      )}

      {screen === "completed-process" && activeProcess && selectedTemplate && (
        <CompletedProcess
          process={activeProcess}
          template={selectedTemplate}
          onMainMenu={() => setScreen("dashboard")}
        />
      )}
    </div>
  );
}
