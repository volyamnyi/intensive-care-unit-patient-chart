import { BrowserRouter, Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { useThemeMode, ThemeModeProvider } from './styles/ThemeContext';
import './styles/animations.css';
import { AuthProvider, useAuth } from './services/AuthContext';
import { useEffect, useRef } from 'react';
import LoginPage from './pages/LoginPage';
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
    if (roles && !hasRole(...roles) && location.pathname !== '/') {
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
  const location = useLocation();
  const redirected = useRef(false);

  useEffect(() => {
    if (redirected.current) return;
    if (user) {
      redirected.current = true;
      const target = user.role === 'NURSE' ? '/nurse'
        : user.role === 'ADMINISTRATOR' ? '/admin'
        : user.role === 'AUDITOR' ? '/admin'
        : '/doctor';
      if (location.pathname !== target) {
        navigate(target, { replace: true });
      }
    }
  });

  if (!user) return null;
  return null;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginRoute />} />

      <Route path="/doctor" element={
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
        <Route path="prescriptions" element={<PrescriptionPage />} />
        <Route path="prescription/:id" element={<PrescriptionDetailPage />} />
      </Route>

      <Route path="/nurse" element={
        <Guard roles={['NURSE']}>
          <NurseLayout />
        </Guard>
      }>
        <Route index element={<NurseDashboardPage />} />
        <Route path="episode/:episodeId" element={<PatientDayPage />} />
        <Route path="prescriptions" element={<NursePrescriptionPage />} />
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
    </Routes>
  );
}

function ThemedApp() {
  const { theme } = useThemeMode();
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
}

export default function App() {
  return (
    <ThemeModeProvider>
      <ThemedApp />
    </ThemeModeProvider>
  );
}
