import { useState } from "react";

interface Props {
  onLogin: (user: string) => void;
}

export function LoginScreen({ onLogin }: Props) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(false);

  const handleLogin = () => {
    if (username && password) {
      if (username === "demo" && password === "demo") {
        onLogin("Коваленко М.В.");
      } else {
        setError(true);
      }
    } else {
      setError(true);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="bg-white border border-gray-300 rounded p-10 w-full max-w-md shadow-sm">
        {/* Logo placeholder */}
        <div className="flex flex-col items-center mb-8">
          <div className="w-20 h-20 bg-gray-200 border-2 border-dashed border-gray-400 rounded flex items-center justify-center mb-3">
            <span className="text-gray-400 text-xs text-center leading-tight">ЛОГОТИП</span>
          </div>
          <h1 className="text-gray-900">Prosthetics Process Management</h1>
          <p className="text-gray-500 text-sm mt-1">Авторизація через Active Directory</p>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-gray-700 text-sm mb-1">Ім'я користувача (AD)</label>
            <input
              type="text"
              value={username}
              onChange={(e) => { setUsername(e.target.value); setError(false); }}
              placeholder="domain\username або demo"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white focus:outline-none focus:border-gray-500"
              onKeyDown={(e) => e.key === "Enter" && handleLogin()}
            />
          </div>
          <div>
            <label className="block text-gray-700 text-sm mb-1">Пароль</label>
            <input
              type="password"
              value={password}
              onChange={(e) => { setPassword(e.target.value); setError(false); }}
              placeholder="••••••••  (demo)"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white focus:outline-none focus:border-gray-500"
              onKeyDown={(e) => e.key === "Enter" && handleLogin()}
            />
          </div>

          {error && (
            <div className="bg-gray-100 border border-gray-400 rounded px-3 py-2 text-sm text-gray-700">
              Невірне ім'я користувача або пароль. Спробуйте ще раз.
            </div>
          )}

          <button
            onClick={handleLogin}
            className="w-full bg-gray-800 text-white py-2 rounded text-sm hover:bg-gray-700 transition-colors"
          >
            Увійти
          </button>
        </div>

        <p className="text-xs text-gray-400 text-center mt-6">
          Для демо: логін <strong>demo</strong> / пароль <strong>demo</strong>
        </p>
      </div>
    </div>
  );
}
