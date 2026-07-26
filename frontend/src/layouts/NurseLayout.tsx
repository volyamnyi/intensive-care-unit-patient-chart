import { Outlet } from 'react-router-dom';
import { Box } from '@mui/material';

export default function NurseLayout() {
  return (
    <Box className="fade-in-up">
      <Outlet />
    </Box>
  );
}
