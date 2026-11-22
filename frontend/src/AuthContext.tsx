import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

interface User { email: string; role: string; fullName: string; token: string; }
interface AuthCtx { user: User | null; login: (u: User) => void; logout: () => void; }

const AuthContext = createContext<AuthCtx>({ user: null, login: () => {}, logout: () => {} });
export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const stored = localStorage.getItem('user');
    if (stored) setUser(JSON.parse(stored));
  }, []);

  const login = (u: User) => {
    localStorage.setItem('token', u.token);
    localStorage.setItem('user', JSON.stringify(u));
    setUser(u);
  };
  const logout = () => {
    localStorage.clear();
    setUser(null);
  };
  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>;
};
