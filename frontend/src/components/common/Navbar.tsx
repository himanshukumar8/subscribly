import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { Badge } from './Badge';
import { LogOut, User as UserIcon } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, isAdmin, logout } = useAuth();

  return (
    <header className="sticky top-0 z-40 flex h-16 items-center justify-between border-b border-slate-800/80 bg-slate-950/80 px-6 backdrop-blur-md">
      <div className="flex items-center gap-3">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-500 to-violet-600 text-white shadow-lg shadow-indigo-500/20 font-black text-lg">
          S
        </div>
        <div>
          <h1 className="text-base font-extrabold tracking-tight text-white">
            Subscribly
          </h1>
          <p className="text-[10px] uppercase font-bold tracking-widest text-indigo-400">
            Cloud SaaS Platform
          </p>
        </div>
      </div>

      <div className="flex items-center gap-4">
        {isAdmin && <Badge variant="info">Admin Privilege</Badge>}

        <div className="flex items-center gap-3 border-l border-slate-800/80 pl-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-800 text-slate-300 font-semibold text-xs border border-slate-700">
              {user?.fullName
                ? user.fullName
                    .split(' ')
                    .map((n) => n[0])
                    .join('')
                    .toUpperCase()
                    .slice(0, 2)
                : <UserIcon className="w-4 h-4" />}
            </div>
            <div className="hidden sm:block text-left">
              <p className="text-xs font-semibold text-slate-200">
                {user?.fullName || user?.email}
              </p>
              <p className="text-[11px] text-slate-400">{user?.email}</p>
            </div>
          </div>

          <button
            onClick={logout}
            title="Log out"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
          >
            <LogOut className="w-4 h-4" />
            <span className="hidden md:inline">Sign Out</span>
          </button>
        </div>
      </div>
    </header>
  );
};
