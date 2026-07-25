import { createContext, useState, useContext } from "react";
import { jwtDecode } from "jwt-decode";

export const AuthContext = createContext({
  user: null,
  handleLogin: (accessToken) => {},
  handleLogin2P: (accessToken) => {},
  handleLogout: () => {},
});

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState({
    id: null,
    firstName: "",
    lastName: "",
    login: "",
    token: "",
    businessRole: "",
  });

  const handleLogin = (accessToken) => {
    const decodedUser = jwtDecode(accessToken);
    localStorage.setItem("sub", decodedUser.sub);
    localStorage.setItem("firstName", decodedUser.firstName);
    localStorage.setItem("lastName", decodedUser.lastName);
    localStorage.setItem("businessRole", decodedUser.businessRole);
    localStorage.setItem("userRole", decodedUser.userRole);
    localStorage.setItem("accessToken", accessToken);
    setUser(decodedUser);
  };

  const handleLogin2P = (accessToken) => {
    const decodedUser = jwtDecode(accessToken);
    localStorage.setItem("sub2P", decodedUser.sub);
    localStorage.setItem("firstName2P", decodedUser.firstName);
    localStorage.setItem("lastName2P", decodedUser.lastName);
    localStorage.setItem("businessRole2P", decodedUser.businessRole);
    localStorage.setItem("userRole2P", decodedUser.userRole);
    localStorage.setItem("accessToken2P", accessToken);
    setUser(decodedUser);
  };

  const handleLogout = () => {
    localStorage.clear();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, handleLogin, handleLogout, handleLogin2P}}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  return useContext(AuthContext);
};
