import React from 'react';
import type { LucideIcon } from 'lucide-react';
import clsx from 'clsx';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  trend?: {
    value: string;
    isPositive: boolean;
  };
  accent?: 'indigo' | 'emerald' | 'amber' | 'violet';
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  icon: Icon,
  trend,
  accent = 'indigo',
}) => {
  const accentClasses = {
    indigo:
      'bg-indigo-500/10 text-indigo-400 border-indigo-500/20 group-hover:border-indigo-500/40',
    emerald:
      'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 group-hover:border-emerald-500/40',
    amber:
      'bg-amber-500/10 text-amber-400 border-amber-500/20 group-hover:border-amber-500/40',
    violet:
      'bg-violet-500/10 text-violet-400 border-violet-500/20 group-hover:border-violet-500/40',
  };

  return (
    <div className="group relative p-6 rounded-2xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-md transition-all duration-300 hover:bg-slate-900/80 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-indigo-500/5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1">
            {title}
          </p>
          <h4 className="text-3xl font-extrabold tracking-tight text-white">
            {value}
          </h4>
          {subtitle && (
            <p className="text-xs text-slate-500 mt-1">{subtitle}</p>
          )}
          {trend && (
            <div className="flex items-center gap-1.5 mt-2">
              <span
                className={clsx(
                  'text-xs font-bold px-2 py-0.5 rounded-full',
                  trend.isPositive
                    ? 'bg-emerald-500/10 text-emerald-400'
                    : 'bg-rose-500/10 text-rose-400'
                )}
              >
                {trend.value}
              </span>
              <span className="text-xs text-slate-400">vs last month</span>
            </div>
          )}
        </div>
        <div
          className={clsx(
            'flex items-center justify-center w-12 h-12 rounded-2xl border transition-colors',
            accentClasses[accent]
          )}
        >
          <Icon className="w-6 h-6" />
        </div>
      </div>
    </div>
  );
};
