import React, { createContext, useContext, useState, useEffect } from 'react';
import type { User } from '../types';
import { authApi } from '../api/authApi';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  loading: boolean;
  login: (token: string, userData?: User) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('subscribly_token'));
  const [loading, setLoading] = useState<boolean>(true);

  const fetchUser = async (authToken: string) => {
    try {
      localStorage.setItem('subscribly_token', authToken);
      const userData = await authApi.getMe();
      setUser(userData);
      setToken(authToken);
    } catch (err) {
      console.error('Failed to authenticate user session:', err);
      localStorage.removeItem('subscribly_token');
      setToken(null);
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const existingToken = localStorage.getItem('subscribly_token');
    if (existingToken) {
      fetchUser(existingToken);
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (newToken: string, userData?: User) => {
    localStorage.setItem('subscribly_token', newToken);
    setToken(newToken);
    if (userData) {
      setUser(userData);
    } else {
      await fetchUser(newToken);
    }
  };

  const logout = () => {
    localStorage.removeItem('subscribly_token');
    setToken(null);
    setUser(null);
  };

  const refreshUser = async () => {
    if (token) {
      await fetchUser(token);
    }
  };

  const isAdmin = Boolean(
    user?.roles?.some((role) => role === 'ROLE_ADMIN' || role === 'ADMIN')
  );

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user && !!token,
        isAdmin,
        loading,
        login,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
