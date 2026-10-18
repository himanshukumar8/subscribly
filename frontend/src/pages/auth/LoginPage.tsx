import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { authApi } from '../../api/authApi';
import { Button } from '../../components/common/Button';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { Shield, Sparkles, Lock, Mail } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await authApi.login(email, password);
      await login(res.token, res.user);
      navigate('/');
    } catch (err: any) {
      console.error('Login error:', err);
      setError(
        err.response?.data?.message ||
          'Invalid email or password. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  const autofillDemoUser = () => {
    setEmail('demo@subscribly.app');
    setPassword('Password123!');
  };

  const autofillAdminUser = () => {
    setEmail('admin@subscribly.app');
    setPassword('Admin123!');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 p-4 relative overflow-hidden">
      {/* Decorative Glowing Backdrop */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-violet-600/20 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-md p-8 rounded-3xl bg-slate-900/80 border border-slate-800/80 shadow-2xl backdrop-blur-xl animate-slide-up">
        <div className="flex flex-col items-center text-center mb-8">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-500 to-violet-600 text-white shadow-lg shadow-indigo-500/25 font-black text-2xl mb-4">
            S
          </div>
          <h2 className="text-2xl font-extrabold tracking-tight text-white">
            Welcome back to Subscribly
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Sign in to your SaaS dashboard
          </p>
        </div>

        <ErrorAlert message={error} onDismiss={() => setError('')} />

        <form onSubmit={handleSubmit} className="space-y-4 mt-6">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
              Email Address
            </label>
            <div className="relative">
              <Mail className="absolute left-3.5 top-3 w-4 h-4 text-slate-500" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@example.com"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-10 pr-4 py-2.5 text-sm text-slate-100 placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
              Password
            </label>
            <div className="relative">
              <Lock className="absolute left-3.5 top-3 w-4 h-4 text-slate-500" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-10 pr-4 py-2.5 text-sm text-slate-100 placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
              />
            </div>
          </div>

          <div className="pt-2">
            <Button
              type="submit"
              variant="primary"
              size="lg"
              isLoading={loading}
              className="w-full"
            >
              Sign In to Account
            </Button>
          </div>
        </form>

        {/* Demo Helpers for Interview / Testing */}
        <div className="mt-8 pt-6 border-t border-slate-800/80">
          <p className="text-xs text-center font-medium text-slate-400 mb-3">
            Quick Test / Interview Credentials:
          </p>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={autofillDemoUser}
              className="flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-800/80 hover:bg-slate-800 text-slate-300 border border-slate-700/80 transition-all"
            >
              <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
              Demo User
            </button>
            <button
              type="button"
              onClick={autofillAdminUser}
              className="flex items-center justify-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-800/80 hover:bg-slate-800 text-slate-300 border border-slate-700/80 transition-all"
            >
              <Shield className="w-3.5 h-3.5 text-amber-400" />
              Admin User
            </button>
          </div>
        </div>

        <p className="text-xs text-center text-slate-400 mt-6">
          Don't have an account yet?{' '}
          <Link
            to="/register"
            className="text-indigo-400 font-semibold hover:text-indigo-300 transition-colors"
          >
            Create an account
          </Link>
        </p>
      </div>
    </div>
  );
};
