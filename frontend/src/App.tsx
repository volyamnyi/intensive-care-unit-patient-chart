import { BrowserRouter, Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { useEffect, useRef } from 'react';
import { ThemeModeProvider } from './styles/ThemeContext';
import { TooltipProvider } from '@/components/ui/tooltip';
import { AuthProvider, useAuth } from './services/AuthContext';
import LoginPage from './pages/LoginPage';
import GlobalLayout from './layouts/GlobalLayout';
import DoctorLayout from './layouts/DoctorLayout';
import NurseLayout from './layouts/NurseLayout';
import DashboardPage from './pages/doctor/DashboardPage';
import CreateCardPage from './pages/doctor/CreateCardPage';
import PatientDayPage from './pages/doctor/PatientDayPage';
import DepartmentDashboardPage from './pages/doctor/DepartmentDashboardPage';
import NurseDashboardPage from './pages/nurse/NurseDashboardPage';
import AdminPage from './pages/admin/AdminPage';
import PrescriptionPage from './pages/prescription/PrescriptionPage';
import PrescriptionDetailPage from './pages/prescription/PrescriptionDetailPage';
import NursePrescriptionPage from './pages/prescription/NursePrescriptionPage';
import AppSelectorPage from './pages/AppSelectorPage';
import ProstheticsDashboard from './pages/prosthetics/DashboardPage';
import PatientSearchPage from './pages/prosthetics/setup/PatientSearchPage';
import OrderSelectPage from './pages/prosthetics/setup/OrderSelectPage';
import OrderReviewPage from './pages/prosthetics/setup/OrderReviewPage';
import TemplateSelectPage from './pages/prosthetics/setup/TemplateSelectPage';
import ProcessLayout from './pages/prosthetics/process/ProcessLayout';
import ProcessDetail from './pages/prosthetics/process/ProcessDetail';
import ProcessHistoryPage from './pages/prosthetics/process/ProcessHistoryPage';
import WizardScreen from './pages/prosthetics/process/WizardScreen';
import DoneScreen from './pages/prosthetics/process/DoneScreen';
import FailedScreen from './pages/prosthetics/process/FailedScreen';
import { ProstheticsProvider } from './prosthetics/ProstheticsContext';

function Guard({ children, roles }: { children: React.ReactNode; roles?: string[] }) {
  const { isAuthenticated, hasRole, user, loading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const redirected = useRef(false);

  useEffect(() => {
    if (redirected.current) return;
    if (loading) return;
    if (!isAuthenticated && location.pathname !== '/login') {
      redirected.current = true;
      navigate('/login', { replace: true });
      return;
    }
    if (roles && user === null) return;
    if (roles && !hasRole(...roles) && location.pathname !== '/' && location.pathname !== '/select') {
      redirected.current = true;
      navigate('/', { replace: true });
    }
  });

  if (loading) return null;
  if (!isAuthenticated) return null;
  if (roles && user === null) return null;
  if (roles && !hasRole(...roles)) return null;
  return <>{children}</>;
}

function LoginRoute() {
  const { isAuthenticated, loading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const redirected = useRef(false);

  useEffect(() => {
    if (redirected.current) return;
    if (loading) return;
    if (isAuthenticated && location.pathname === '/login') {
      redirected.current = true;
      navigate('/', { replace: true });
    }
  });

  if (loading) return null;
  if (isAuthenticated) return null;
  return <LoginPage />;
}

function RoleRedirect() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const redirected = useRef(false);

  useEffect(() => {
    if (redirected.current) return;
    if (user) {
      redirected.current = true;
      navigate('/select', { replace: true });
    }
  });

  if (!user) return null;
  return null;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginRoute />} />

      <Route element={<GlobalLayout />}>
        <Route path="/select" element={
          <Guard>
            <AppSelectorPage />
          </Guard>
        } />

        <Route path="/icu">
          <Route path="doctor" element={
            <Guard roles={['DOCTOR', 'HEAD_OF_DEPARTMENT']}>
              <DoctorLayout />
            </Guard>
          }>
            <Route index element={<DashboardPage />} />
            <Route path="department" element={
              <Guard roles={['HEAD_OF_DEPARTMENT']}>
                <DepartmentDashboardPage />
              </Guard>
            } />
            <Route path="create-card" element={<CreateCardPage />} />
            <Route path="episode/:episodeId" element={<PatientDayPage />} />
          </Route>

          <Route path="nurse" element={
            <Guard roles={['NURSE']}>
              <NurseLayout />
            </Guard>
          }>
            <Route index element={<NurseDashboardPage />} />
            <Route path="episode/:episodeId" element={<PatientDayPage />} />
          </Route>
        </Route>

        <Route path="/prescriptions">
          <Route path="doctor" element={
            <Guard roles={['DOCTOR', 'HEAD_OF_DEPARTMENT']}>
              <PrescriptionPage />
            </Guard>
          } />
          <Route path="doctor/:id" element={
            <Guard roles={['DOCTOR', 'HEAD_OF_DEPARTMENT']}>
              <PrescriptionDetailPage />
            </Guard>
          } />
          <Route path="nurse" element={
            <Guard roles={['NURSE']}>
              <NursePrescriptionPage />
            </Guard>
          } />
          <Route path="nurse/:id" element={
            <Guard roles={['NURSE']}>
              <PrescriptionDetailPage />
            </Guard>
          } />
        </Route>

        <Route path="/prosthetics" element={
          <Guard roles={['PROSTHETIST', 'PROSTHETICS_ADMINISTRATOR', 'HEAD_OF_DEPARTMENT']}>
            <ProstheticsProvider>
              <ProstheticsDashboard />
            </ProstheticsProvider>
          </Guard>
        } />

        <Route path="/prosthetics/new">
          <Route path="select-patient" element={
            <Guard roles={['PROSTHETIST', 'PROSTHETICS_ADMINISTRATOR', 'HEAD_OF_DEPARTMENT']}>
              <ProstheticsProvider>
                <PatientSearchPage />
              </ProstheticsProvider>
            </Guard>
          } />
          <Route path="select-order" element={
            <Guard roles={['PROSTHETIST', 'PROSTHETICS_ADMINISTRATOR', 'HEAD_OF_DEPARTMENT']}>
              <ProstheticsProvider>
                <OrderSelectPage />
              </ProstheticsProvider>
            </Guard>
          } />
          <Route path="review-order" element={
            <Guard roles={['PROSTHETIST', 'PROSTHETICS_ADMINISTRATOR', 'HEAD_OF_DEPARTMENT']}>
              <ProstheticsProvider>
                <OrderReviewPage />
              </ProstheticsProvider>
            </Guard>
          } />
          <Route path="select-template" element={
            <Guard roles={['PROSTHETIST', 'PROSTHETICS_ADMINISTRATOR', 'HEAD_OF_DEPARTMENT']}>
              <ProstheticsProvider>
                <TemplateSelectPage />
              </ProstheticsProvider>
            </Guard>
          } />
        </Route>

        <Route path="/prosthetics/process/:id" element={
          <Guard roles={['PROSTHETIST', 'PROSTHETICS_ADMINISTRATOR', 'HEAD_OF_DEPARTMENT']}>
            <ProstheticsProvider>
              <ProcessLayout />
            </ProstheticsProvider>
          </Guard>
        }>
          <Route index element={<ProcessDetail />} />
          <Route path="history" element={<ProcessHistoryPage />} />
          <Route path="wizard" element={<WizardScreen />} />
          <Route path="done" element={<DoneScreen />} />
          <Route path="failed" element={<FailedScreen />} />
        </Route>

        <Route path="/admin" element={
          <Guard roles={['ADMINISTRATOR', 'AUDITOR']}>
            <AdminPage />
          </Guard>
        } />

        <Route path="/" element={
          <Guard>
            <RoleRedirect />
          </Guard>
        } />
      </Route>
    </Routes>
  );
}

function ThemedApp() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <TooltipProvider>
          <AppRoutes />
        </TooltipProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default function App() {
  return (
    <ThemeModeProvider>
      <ThemedApp />
    </ThemeModeProvider>
  );
}
