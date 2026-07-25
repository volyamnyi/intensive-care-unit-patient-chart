import React, { useState, useEffect } from "react";
import { userRegister,getAllUsers, updateUserById, deleteUserById } from "../../utils/ApiFunctions";

export default function Admin() {
  const [users, setUsers] = useState([
    {
      id: "",
      login: "",
      password: "",
      firstName: "",
      lastName: "",
      middleName: "",
      password_confirm: "",
      businessRole: "",
    },
  ]);

  const [modalOpen, setModalOpen] = useState(false);
  const [editUser, setEditUser] = useState(null);
  const [newUser, setNewUser] = useState({
    login: "",
    password: "",
    firstName: "",
    lastName: "",
    middleName: "",
    password_confirm: "",
    userRole: "",
    businessRole: "",
  });
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const [changeState, setChangeState] = useState(0)

  async function handleAddUser(e) {
    e.preventDefault();
    /*if (registration.password !== registration.password_confirm) {
      setErrorMessage("Registration error : passwords do not match");
      setTimeout(() => {
        setErrorMessage("");
        setSuccessMessage("");
      }, 5000);
      return;
    }*/
    try {
      const result = await userRegister(newUser);
      setSuccessMessage("Registration successful");
      setErrorMessage("");
      newUser({
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
    setNewUser({
      login: "",
      password: "",
      firstName: "",
      lastName: "",
      middleName: "",
      password_confirm: "",
      userRole: "",
    });
    setModalOpen(false);
    setChangeState(prevValue=>prevValue+1)
  }

  async function handleEditUser(e)  {
    e.preventDefault();
    //setUsers(users.map((u) => (u.id === editUser.id ? editUser : u)));
    //console.log(editUser)
    updateUserById(editUser)
    setEditUser(null);
    setModalOpen(false);
    
    setChangeState(prevValue=>prevValue+1)
  };

  const handleDeleteUser = (e,id) => {
    e.preventDefault();
    //setUsers(users.filter((u) => u.id !== id));
    deleteUserById(id)
    
    setChangeState(prevValue=>prevValue+1)

  };

  const [showPassword, setShowPassword] = useState(false);

  const handleToggleShowPassword = () => {
    setShowPassword(!showPassword);
  };

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        const result = await getAllUsers();
        setUsers(result); 
      } catch (error) {
        console.error("Error fetching users:", error);
      }
    };

    fetchUsers();
  }, [changeState]);


  return (
    <>
      <style>
        {`
          .admin-container {
            max-width: 900px;
            margin: 50px auto;
            padding: 20px;
            border: 1px solid #ddd;
            border-radius: 8px;
            box-shadow: 2px 2px 10px rgba(0,0,0,0.1);
            text-align: center;
          }
          table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
          }
          th, td {
            padding: 10px;
            border: 1px solid #ddd;
            text-align: left;
          }
          th {
            background: #f4f4f4;
          }
          .btn {
            padding: 8px 12px !important;
            margin: 5px !important;
            border: none !important;
            cursor: pointer !important;
            border-radius: 4px !important;
          }
          .btn-add { background: #28a745 !important; color: white; }
          .btn-edit { background: #ffc107 !important; color: black; }
          .btn-delete { background: #dc3545 !important; color: white; }
          .modal {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: white;
            padding: 20px;
            box-shadow: 0px 4px 10px rgba(0,0,0,0.1);
            border-radius: 8px;
          }
          .modal input {
            width: 100%;
            padding: 8px;
            margin-top: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
          }
          .modal-buttons {
            margin-top: 15px;
          }
          .roles-container {
            margin-top:10px;
            display:flex;
            column-gap:5px;
          }

          .toggle-password {
            cursor: pointer;
            position: absolute;
            margin-left: -30px;
            margin-top: 10px;
            font-size: 20px;
          }

        `}
      </style>
      <form onSubmit={handleAddUser}>
        <div className="admin-container">
          <h2>Адміністративна панель</h2>

          <button
            type="button"
            className="btn btn-add"
            onClick={() => setModalOpen(true)}
          >
            Додати користувача
          </button>

          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Ім'я</th>
                <th>Логін</th>
                <th>Бізнес роль</th>
                <th>Основна роль</th>
                <th>Дії</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.id}</td>
                  <td>{user.firstName + " " + user.lastName}</td>
                  <td>{user.login}</td>
                  <td>{user.businessRole}</td>
                  <td>{user.userRole}</td>
                  <td>
                    <button
                      type="button"
                      className="btn btn-edit"
                      onClick={() => {
                        setEditUser(user);
                        setModalOpen(true);
                      }}
                    >
                      Редагувати
                    </button>
                    <button
                      className="btn btn-delete"
                      type="button"
                      onClick={(e) => handleDeleteUser(e,user.id)}
                    >
                      Видалити
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </form>
      {/* Модальне вікно */}
      {modalOpen && (
        <div className="modal">
          <h3>{editUser ? "Редагувати користувача" : "Додати користувача"}</h3>
          <input
            type="text"
            placeholder="Ім'я"
            value={editUser ? editUser.firstName : newUser.firstName}
            onChange={(e) => {
              if (editUser)
                setEditUser({ ...editUser, firstName: e.target.value });
              else setNewUser({ ...newUser, firstName: e.target.value });
            }}
          />
          <input
            type="text"
            placeholder="Прізвище"
            value={editUser ? editUser.lastName : newUser.lastName}
            onChange={(e) => {
              if (editUser)
                setEditUser({ ...editUser, lastName: e.target.value });
              else setNewUser({ ...newUser, lastName: e.target.value });
            }}
          />
          <input
            type="text"
            placeholder="По-батькові"
            value={editUser ? editUser.middleName : newUser.middleName}
            onChange={(e) => {
              if (editUser)
                setEditUser({ ...editUser, middleName: e.target.value });
              else setNewUser({ ...newUser, middleName: e.target.value });
            }}
          />
          <input
            type="text"
            placeholder="Логін"
            value={editUser ? editUser.login : newUser.login}
            onChange={(e) => {
              if (editUser) setEditUser({ ...editUser, login: e.target.value });
              else setNewUser({ ...newUser, login: e.target.value });
            }}
          />
          <input
            type={showPassword ? "text" : "password"}
            placeholder="Пароль"
            name="password"
            value={editUser ? editUser.password : newUser.password}
            onChange={(e) => {
              if (editUser)
                setEditUser({ ...editUser, password: e.target.value });
              else setNewUser({ ...newUser, password: e.target.value });
            }}
          />
          <span
            className={`toggle-password ${showPassword ? "eye" : "eye-off"}`}
            onClick={handleToggleShowPassword}
          >
            {showPassword ? "x" : "👁"}
          </span>
          <div className="roles-container">
            <label>
              Бізнес роль:
              <span className="login-danger">*</span>
            </label>
            <select
              required
              id="businessRole"
              name="businessRole"
              value={editUser ? editUser.businessRole : newUser.businessRole}
              onChange={(e) => {
                if (editUser)
                  setEditUser({ ...editUser, businessRole: e.target.value });
                else setNewUser({ ...newUser, businessRole: e.target.value });
              }}
            >
              <option value="">Обрати роль</option>
              <option value="DOCTOR">DOCTOR</option>
              <option value="NURSE">NURSE</option>
            </select>
            <label>
              Основна роль:
              <span className="login-danger">*</span>
            </label>
            <select
              required
              id="userRole"
              name="userRole"
              value={editUser ? editUser.userRole : newUser.userRole}
              onChange={(e) => {
                if (editUser)
                  setEditUser({ ...editUser, userRole: e.target.value });
                else setNewUser({ ...newUser, userRole: e.target.value });
              }}
            >
              <option value="">Обрати роль</option>
              <option value="ADMIN">ADMIN</option>
              <option value="EMPLOYEE">EMPLOYEE</option>
            </select>
          </div>
          <div className="modal-buttons">
            <button
              type="button"
              className="btn btn-add"
              onClick={editUser ? (e)=>handleEditUser(e) : (e)=>handleAddUser(e)}
            >
              {editUser ? "Зберегти" : "Додати"}
            </button>
            <button
                type="button"
              className="btn btn-delete"
              onClick={() => {
                setModalOpen(false);
                setEditUser(null);
              }}
            >
              Закрити
            </button>
          </div>
        </div>
      )}
    </>
  );
}
