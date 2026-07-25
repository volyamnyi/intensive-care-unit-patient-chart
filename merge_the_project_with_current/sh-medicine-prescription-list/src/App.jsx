import React from "react";
import {
  Routes,
  Route,
  useNavigate,
  useLocation,
  Navigate,
} from "react-router-dom";

import Home from "./component/Home";
import Main from "./component/Main";
import ListDetails from "./component/MedicineList/ListDetails";
import PatientDetails from "./component/MedicineList/PatientDetails";
import NewList from "./component/MedicineList/NewList";
import TemperatureList from "./component/MedicineList/TemperatureList";
import { AuthContext } from "./component/auth/AuthProvider";
import Registration from "./component/auth/Registration";
import Login from "./component/auth/Login";
import Login2P from "./component/auth/Login2P";
import Admin from "./component/admin/Admin";
import RequireAuth from "./component/auth/RequireAuth";
import _403 from "./component/auth/_403";
import ListPrint from "./component/MedicineList/ListPrint";
import MedicineListPrint from "./component/MedicineList/MedicineListPrint";

function App() {
  const auth = React.useContext(AuthContext);
  const isLoggedIn = localStorage.getItem("accessToken");
  const navigate = useNavigate();

  const handleLogout = () => {
    auth.handleLogout();
    navigate("/", { state: { message: " You have been logged out!" } });
  };

  return (
    <>
      <Main>
        &nbsp;&nbsp;&nbsp;<img src="/superhumans.svg" width={100}></img>
        <br></br>
        {isLoggedIn && useLocation().pathname === "/" && (
          <a className="dropdown-item" href="#" onClick={handleLogout}>
            &nbsp;&nbsp;&nbsp; Вийти
          </a>
        )}
        <Routes>
          <Route index element={!isLoggedIn ? <Login /> : <Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/login2P" element={<Login2P />} />
          <Route path="/register" element={<Registration />} />
          <Route path="/temperaturelist" element={<TemperatureList />} />
          <Route
            path="/admin"
            element={
              localStorage.getItem("userRole") === "ADMIN" ? (
                <Admin />
              ) : (
                <_403 />
              )
            }
          />

          <Route path="*" element={<Navigate to="/forbidden" />} />
          <Route path="/forbidden" element={<_403 />} />
          <Route
            path="/"
            element={
              <RequireAuth>
                <Home />
              </RequireAuth>
            }
          />
          <Route
            path="/:id"
            element={
              <RequireAuth>
                <Home />
              </RequireAuth>
            }
          />
          <Route
            path="/listdetails/:id"
            element={
              <RequireAuth>
                <ListDetails />
              </RequireAuth>
            }
          />
          <Route
            path="/listdetails/copy/:id"
            element={
              <RequireAuth>
                <ListDetails isCopy={true} />
              </RequireAuth>
            }
          />

          <Route
            path="/listdetails/:id/listprint"
            element={
              <RequireAuth>
                <ListPrint />
              </RequireAuth>
            }
          />
          <Route
            path="/listdetails/:id/medicinelistprint"
            element={
              <RequireAuth>
                <MedicineListPrint />
              </RequireAuth>
            }
          />
          <Route
            path="/patientdetails/:id"
            element={
              <RequireAuth>
                <PatientDetails />
              </RequireAuth>
            }
          />
          <Route
            path="/newlist/:id/:documentName"
            element={
              <RequireAuth>
                <NewList />
              </RequireAuth>
            }
          />
        </Routes>
      </Main>
    </>
  );
}

export default App;
