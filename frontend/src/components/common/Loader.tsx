import React from 'react';
import { Loader2 } from 'lucide-react';
import clsx from 'clsx';

interface LoaderProps {
  size?: 'sm' | 'md' | 'lg';
  label?: string;
  className?: string;
}

export const Loader: React.FC<LoaderProps> = ({
  size = 'md',
  label,
  className,
}) => {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-8 h-8',
    lg: 'w-12 h-12',
  };

  return (
    <div
      className={clsx(
        'flex flex-col items-center justify-center gap-3 text-slate-400',
        className
      )}
    >
      <Loader2
        className={clsx('animate-spin text-indigo-500', sizeClasses[size])}
      />
      {label && <p className="text-sm font-medium animate-pulse">{label}</p>}
    </div>
  );
};
