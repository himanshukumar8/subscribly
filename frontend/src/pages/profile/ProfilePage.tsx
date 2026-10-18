import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import {
  Mail,
  Shield,
  Key,
  RefreshCw,
  CheckCircle2,
} from 'lucide-react';

export const ProfilePage: React.FC = () => {
  const { user, isAdmin, refreshUser, loading } = useAuth();

  return (
    <div className="space-y-8 max-w-4xl animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-black text-white tracking-tight">
            User Profile
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Manage your personal Subscribly SaaS account and credentials.
          </p>
        </div>
        <Button
          variant="secondary"
          size="sm"
          onClick={refreshUser}
          isLoading={loading}
          leftIcon={<RefreshCw className="w-4 h-4" />}
        >
          Refresh Profile
        </Button>
      </div>

      {/* Main Profile Card */}
      <div className="p-8 rounded-3xl bg-slate-900/70 border border-slate-800/80 backdrop-blur-md shadow-xl">
        <div className="flex flex-col sm:flex-row items-center gap-6 pb-8 border-b border-slate-800/80">
          <div className="flex h-20 w-20 items-center justify-center rounded-3xl bg-gradient-to-tr from-indigo-500 to-violet-600 text-white shadow-xl shadow-indigo-500/20 font-black text-3xl">
            {user?.fullName
              ? user.fullName
                  .split(' ')
                  .map((n) => n[0])
                  .join('')
                  .toUpperCase()
                  .slice(0, 2)
              : 'U'}
          </div>
          <div className="text-center sm:text-left">
            <div className="flex flex-wrap items-center justify-center sm:justify-start gap-3">
              <h3 className="text-2xl font-extrabold text-white">
                {user?.fullName || 'Anonymous User'}
              </h3>
              <Badge variant={isAdmin ? 'info' : 'default'}>
                {isAdmin ? 'Administrator' : 'Standard User'}
              </Badge>
            </div>
            <p className="text-sm text-slate-400 mt-1">{user?.email}</p>
            <p className="text-xs text-indigo-400 mt-2 font-mono">
              User ID: #{user?.id}
            </p>
          </div>
        </div>

        {/* Profile Attributes Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-8">
          <div className="p-5 rounded-2xl bg-slate-950/60 border border-slate-800/80">
            <div className="flex items-center gap-3 text-slate-400 mb-2">
              <Mail className="w-4 h-4 text-indigo-400" />
              <span className="text-xs font-semibold uppercase tracking-wider">
                Email Address
              </span>
            </div>
            <p className="text-base font-bold text-white">{user?.email}</p>
            <div className="flex items-center gap-1.5 mt-3 text-xs text-emerald-400">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Verified Identity</span>
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-slate-950/60 border border-slate-800/80">
            <div className="flex items-center gap-3 text-slate-400 mb-2">
              <Shield className="w-4 h-4 text-violet-400" />
              <span className="text-xs font-semibold uppercase tracking-wider">
                Assigned Roles
              </span>
            </div>
            <div className="flex flex-wrap gap-2 mt-1">
              {user?.roles && user.roles.length > 0 ? (
                user.roles.map((role) => (
                  <Badge key={role} variant="info">
                    {role}
                  </Badge>
                ))
              ) : (
                <Badge variant="default">ROLE_USER</Badge>
              )}
            </div>
            <p className="text-xs text-slate-500 mt-3">
              Managed by user-service (Port 8081)
            </p>
          </div>

          <div className="p-5 rounded-2xl bg-slate-950/60 border border-slate-800/80 md:col-span-2">
            <div className="flex items-center gap-3 text-slate-400 mb-2">
              <Key className="w-4 h-4 text-amber-400" />
              <span className="text-xs font-semibold uppercase tracking-wider">
                JWT Authentication Security
              </span>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Your session is protected via stateless HMAC-SHA256 JWT tokens. Tokens are automatically verified by downstream microservices without querying database read-replicas.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
