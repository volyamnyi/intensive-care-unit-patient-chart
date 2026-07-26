import { Outlet } from 'react-router-dom';
import { Box } from '@mui/material';

export default function DoctorLayout() {
  return (
    <Box className="fade-in-up">
      <Outlet />
    </Box>
  );
}
