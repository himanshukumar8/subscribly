import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { AppLayout } from './components/layout/AppLayout';
import { ProtectedRoute } from './components/protected/ProtectedRoute';
import { AdminRoute } from './components/protected/AdminRoute';

// Pages
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { SubscriptionsPage } from './pages/subscriptions/SubscriptionsPage';
import { PaymentsPage } from './pages/payments/PaymentsPage';
import { ProfilePage } from './pages/profile/ProfilePage';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Authentication Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Protected Routes inside AppLayout */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/subscriptions" element={<SubscriptionsPage />} />
              <Route path="/payments" element={<PaymentsPage />} />
              <Route path="/profile" element={<ProfilePage />} />

              {/* Admin Exclusives */}
              <Route element={<AdminRoute />}>
                <Route path="/admin" element={<AdminDashboardPage />} />
              </Route>
            </Route>
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
