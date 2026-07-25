import React, { useState } from "react";
import { Link } from "react-router-dom";
import { userRegister } from "../../utils/ApiFunctions";

const Registration = () => {
  const [registration, setRegistration] = useState({
    login: "",
    password: "",
    firstName: "",
    lastName: "",
    middleName: "",
    password_confirm: "",
    userRole: "",
  });

  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const handleToggleShowPassword = () => {
    setShowPassword(!showPassword);
  };

  const handleInputChange = (e) => {
    setRegistration({ ...registration, [e.target.name]: e.target.value });
  };

  const handleRegistration = async (e) => {
    e.preventDefault();
    if (registration.password !== registration.password_confirm) {
      setErrorMessage("Registration error : passwords do not match");
      setTimeout(() => {
        setErrorMessage("");
        setSuccessMessage("");
      }, 5000);
      return;
    }
    try {
      const result = await userRegister(registration);
      setSuccessMessage("Registration successful");
      setErrorMessage("");
      setRegistration({
        firstName: "",
        lastName: "",
        email: "",
        password: "",
        password_confirm: "",
        businessRole: "",
        userRole,
      });
    } catch (error) {
      setSuccessMessage("");
      setErrorMessage(`Registration error : ${error.message}`);
    }
    setTimeout(() => {
      setErrorMessage("");
      setSuccessMessage("");
    }, 5000);
  };
  const handleSelectUserRoleChange = (event) => {
    const name = event.target.name;
    const value = event.target.value;

    setRegistration({ ...registration, [name]: value });
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
          .margin-bottom {
            margin-bottom: 10px;
          }
        `}
      </style>
      <div className="login-container">
        <div className="login-box">
          <img src="/superhumans.svg" alt="Logo" className="logo" />

          <div>
            <div>
              {errorMessage && (
                <p className="alert alert-danger">{errorMessage}</p>
              )}
              {successMessage && (
                <p className="alert alert-success">{successMessage}</p>
              )}
              <h1>Реєстрація</h1>
              <p>Введіть дані користувача:</p>
              <form onSubmit={handleRegistration}>
                <div className="margin-bottom">
                  <label>
                    Бізнес роль:
                    <span className="login-danger">*</span>
                  </label>
                  <select
                    required
                    id="businessRole"
                    name="businessRole"
                    value={registration.businessRole}
                    onChange={handleSelectUserRoleChange}
                  >
                    <option value="">Обрати роль</option>
                    <option value="DOCTOR">DOCTOR</option>
                    <option value="NURSE">NURSE</option>
                  </select>
                </div>
                <div>
                  <label>
                    Роль користувача:
                    <span className="login-danger">*</span>
                  </label>
                  <select
                    required
                    id="userRole"
                    name="userRole"
                    value={registration.userRole}
                    onChange={handleSelectUserRoleChange}
                  >
                    <option value="">Обрати роль</option>
                    <option value="ADMIN">ADMIN</option>
                    <option value="EMPLOYEE">EMPLOYEE</option>
                  </select>
                </div>
                <div>
                  <label>
                    Логін <span className="login-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="login"
                    name="login"
                    onChange={handleInputChange}
                    value={registration.login}
                  />
                </div>
                <div>
                  <label>
                    Ім'я <span className="login-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="firstName"
                    name="firstName"
                    onChange={handleInputChange}
                    value={registration.firstName}
                  />
                </div>
                <div>
                  <label>
                    Прізвище <span className="login-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="lastName"
                    name="lastName"
                    onChange={handleInputChange}
                    value={registration.lastName}
                  />
                </div>

                <div>
                  <label>
                    По-батькові <span className="login-danger">*</span>
                  </label>
                  <input
                    type="text"
                    id="middleName"
                    name="middleName"
                    onChange={handleInputChange}
                    value={registration.middleName}
                  />
                </div>

                <div>
                  <label>
                    Пароль <span className="login-danger">*</span>
                  </label>
                  <input
                    type={showPassword ? "text" : "password"}
                    id="password"
                    name="password"
                    onChange={handleInputChange}
                    value={registration.password}
                  />
                  <span
                    className={`toggle-password ${
                      showPassword ? "eye" : "eye-off"
                    }`}
                    onClick={handleToggleShowPassword}
                  >
                    {showPassword ? "x" : "👁"}
                  </span>
                </div>
                <div>
                  <label>
                    Підтвердіть пароль <span className="login-danger">*</span>
                  </label>
                  <input
                    type={showPassword ? "text" : "password"}
                    name="password_confirm"
                    onChange={handleInputChange}
                    value={registration.password_confirm}
                  />
                  <span
                    className={`toggle-password ${
                      showPassword ? "eye" : "eye-off"
                    }`}
                    onClick={handleToggleShowPassword}
                  >
                    {showPassword ? "x" : "👁"}
                  </span>
                </div>
                <div className="margin-bottom">
                  Вже зареєстровані? <Link to="/login">Увійти</Link>
                </div>
                <div>
                  <button type="submit">Зареєструватись</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default Registration;
