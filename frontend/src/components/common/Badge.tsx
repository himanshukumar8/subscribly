import React from 'react';
import clsx from 'clsx';

interface BadgeProps {
  variant?: 'success' | 'warning' | 'danger' | 'info' | 'default';
  children: React.ReactNode;
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({
  variant = 'default',
  children,
  className,
}) => {
  const variants = {
    success:
      'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-sm shadow-emerald-500/5',
    warning:
      'bg-amber-500/10 text-amber-400 border border-amber-500/20 shadow-sm shadow-amber-500/5',
    danger:
      'bg-rose-500/10 text-rose-400 border border-rose-500/20 shadow-sm shadow-rose-500/5',
    info: 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 shadow-sm shadow-indigo-500/5',
    default:
      'bg-slate-800 text-slate-300 border border-slate-700/80 shadow-sm',
  };

  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold tracking-wide uppercase rounded-full transition-colors',
        variants[variant],
        className
      )}
    >
      <span
        className={clsx('w-1.5 h-1.5 rounded-full', {
          'bg-emerald-400 animate-pulse': variant === 'success',
          'bg-amber-400': variant === 'warning',
          'bg-rose-400': variant === 'danger',
          'bg-indigo-400': variant === 'info',
          'bg-slate-400': variant === 'default',
        })}
      />
      {children}
    </span>
  );
};
