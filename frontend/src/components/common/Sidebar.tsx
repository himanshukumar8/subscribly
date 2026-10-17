import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Layers,
  CreditCard,
  User,
  ShieldCheck,
} from 'lucide-react';
import clsx from 'clsx';

export const Sidebar: React.FC = () => {
  const { isAdmin } = useAuth();

  const navItems = [
    { label: 'Dashboard', to: '/', icon: LayoutDashboard },
    { label: 'Subscriptions', to: '/subscriptions', icon: Layers },
    { label: 'Payments', to: '/payments', icon: CreditCard },
    { label: 'My Profile', to: '/profile', icon: User },
  ];

  if (isAdmin) {
    navItems.push({
      label: 'Admin Portal',
      to: '/admin',
      icon: ShieldCheck,
    });
  }

  return (
    <aside className="w-64 shrink-0 border-r border-slate-800/80 bg-slate-950/40 p-4 min-h-[calc(100vh-4rem)]">
      <div className="space-y-1">
        <p className="px-3 py-2 text-[11px] font-bold uppercase tracking-widest text-slate-500">
          Main Navigation
        </p>
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                clsx(
                  'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200',
                  isActive
                    ? 'bg-gradient-to-r from-indigo-500/20 to-violet-500/10 text-white border-l-4 border-indigo-500 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                )
              }
            >
              <Icon className="w-5 h-5 shrink-0" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </div>

      <div className="mt-8 p-4 rounded-2xl bg-gradient-to-br from-slate-900 to-slate-900/40 border border-slate-800/80">
        <div className="flex items-center gap-2 text-indigo-400 mb-1">
          <ShieldCheck className="w-4 h-4" />
          <span className="text-xs font-bold uppercase tracking-wide">
            Microservice Backend
          </span>
        </div>
        <p className="text-xs text-slate-400">
          Connected to Spring Cloud API Gateway (Port 8080) with event-driven Kafka outbox & Razorpay verify.
        </p>
      </div>
    </aside>
  );
};
