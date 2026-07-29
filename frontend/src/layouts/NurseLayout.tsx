import { Outlet } from 'react-router-dom';

export default function NurseLayout() {
  return (
    <div className="fade-in-up">
      <Outlet />
    </div>
  );
}
