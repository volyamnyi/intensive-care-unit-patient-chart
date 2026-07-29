import { Outlet } from 'react-router-dom';

export default function DoctorLayout() {
  return (
    <div className="fade-in-up">
      <Outlet />
    </div>
  );
}
