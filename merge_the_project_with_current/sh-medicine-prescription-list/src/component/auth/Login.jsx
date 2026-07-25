import { useState } from "react";
import { userLogin } from "../../utils/ApiFunctions";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthProvider";

const Login = () => {
  const [errorMessage, setErrorMessage] = useState("");
  const [login, setLogin] = useState({
    login: "",
    password: "",
  });

  const navigate = useNavigate();
  const auth = useAuth();
  const location = useLocation();
  const redirectUrl = location.state?.path || "/";
  const [showPassword, setShowPassword] = useState(false);

  const handleToggleShowPassword = () => {
    setShowPassword(!showPassword);
  };

  const handleInputChange = (e) => {
    setLogin({ ...login, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const response = await userLogin(login);
    if (response.status === 200) {
      const token = response.data.token;
     
      auth.handleLogin(token);
    
      navigate(redirectUrl, { replace: true });
    } else {
      setErrorMessage("Невірний логін або пароль.");
    }
    setTimeout(() => {
      setErrorMessage("");
    }, 4000);
  };

  

  return (
    <>
     <style>
        {`
          .login-container {
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            background-color: #f4f4f4;
          }
          .login-box {
            background: white;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0px 4px 10px rgba(0, 0, 0, 0.1);
            text-align: center;
            width: 350px;
          }
          .login-box img {
            width: 100px;
            margin-bottom: 15px;
          }
          .login-box h1 {
            font-size: 24px;
            margin-bottom: 10px;
          }
          .login-box p {
            font-size: 14px;
            margin-bottom: 15px;
          }
          .login-box input {
            width: 100%;
            padding: 10px;
            margin-bottom: 15px;
            border: 1px solid #ccc;
            border-radius: 5px;
            font-size: 16px;
          }
          .login-box button {
            width: 100%;
            padding: 10px;
            background-color: #007bff;
            color: white;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 16px;
          }
          .login-box button:hover {
            background-color: #0056b3;
          }
          .login-box a {
            display: block;
            margin-top: 10px;
            color: #007bff;
            text-decoration: none;
          }
          .login-box a:hover {
            text-decoration: underline;
          }
          .toggle-password {
            cursor: pointer;
            position: absolute;
            margin-left: -30px;
            margin-top: 5px;
            font-size: 20px;
          }
          .logo {
            max-width:50%;
          }
          .forgot-password {
            margin-bottom: 10px;
          }
        `}
      </style>
      <div className="login-container">
        <div className="login-box">
          <img src="/superhumans.svg" alt="Logo" className="logo"/>
          {errorMessage && <p style={{ color: "red" }}>{errorMessage}</p>}
          {/*<p>
            Потрібен аккаунт? <Link to="/register">Реєстрація</Link>
          </p>*/}
         
          <form onSubmit={handleSubmit}>
            <div>
             {/* <label>
                Логін <span className="login-danger">*</span>
              </label>*/}
              <input
                id="email"
                name="login"
                type="text"
                value={login.login}
                onChange={handleInputChange}
                placeholder="Логін"
              />
            </div>
            <div style={{ position: "relative" }}>
              {/*<label>
                Пароль <span className="login-danger">*</span>
              </label>*/}
              <input
                type={showPassword ? "text" : "password"}
                id="password"
                name="password"
                onChange={handleInputChange}
                value={login.password}
                placeholder="Пароль"
              />
              <span
                className={`toggle-password ${
                  showPassword ? "eye" : "eye-off"
                }`}
                onClick={handleToggleShowPassword}
              >
                {showPassword? "x" : "👁"}
              </span>
            </div>
            <div className="forgot-password">
              <Link to="https://sd.superhumans.com/">Забули пароль?</Link>
            </div>
            <div>
              <button type="submit">Увійти</button>
            </div>
          </form>
        </div>
      </div>
    </>
  );
};

export default Login;
