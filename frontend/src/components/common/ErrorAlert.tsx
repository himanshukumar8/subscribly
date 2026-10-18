import React from 'react';
import { AlertCircle, X } from 'lucide-react';

interface ErrorAlertProps {
  message: string;
  onDismiss?: () => void;
}

export const ErrorAlert: React.FC<ErrorAlertProps> = ({
  message,
  onDismiss,
}) => {
  if (!message) return null;

  return (
    <div className="flex items-center justify-between gap-3 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 animate-fade-in shadow-sm">
      <div className="flex items-center gap-3">
        <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />
        <p className="text-sm font-medium">{message}</p>
      </div>
      {onDismiss && (
        <button
          onClick={onDismiss}
          className="p-1 rounded-lg text-rose-400 hover:bg-rose-500/20 transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
      )}
    </div>
  );
};
