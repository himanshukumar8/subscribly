import React, { useEffect, useState } from 'react';
import { authApi } from '../../api/authApi';
import { subscriptionApi } from '../../api/subscriptionApi';
import { paymentApi } from '../../api/paymentApi';
import type { User, Subscription, Payment, PaymentAuditLog } from '../../types';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Loader } from '../../components/common/Loader';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { Modal } from '../../components/common/Modal';
import {
  ShieldCheck,
  Users,
  Layers,
  CreditCard,
  History,
} from 'lucide-react';
import clsx from 'clsx';

export const AdminDashboardPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'users' | 'subscriptions' | 'payments'>('users');
  const [users, setUsers] = useState<User[]>([]);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Audit Logs modal state
  const [selectedPaymentForAudit, setSelectedPaymentForAudit] = useState<Payment | null>(null);
  const [auditLogs, setAuditLogs] = useState<PaymentAuditLog[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);

  const fetchAdminData = async () => {
    setLoading(true);
    setError('');
    try {
      const [uData, sData, pData] = await Promise.all([
        authApi.adminGetAllUsers(),
        subscriptionApi.adminGetAllSubscriptions(),
        paymentApi.adminGetAllPayments(),
      ]);
      setUsers(uData);
      setSubscriptions(sData);
      setPayments(pData);
    } catch (err: any) {
      console.error('Admin data fetch error:', err);
      setError('Failed to fetch platform administrative datasets.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAdminData();
  }, []);

  const handleOpenAuditModal = async (payment: Payment) => {
    setSelectedPaymentForAudit(payment);
    setAuditLoading(true);
    try {
      const logs = await paymentApi.adminGetPaymentAuditLogs(payment.id);
      setAuditLogs(logs);
    } catch (err) {
      console.error('Audit logs fetch error:', err);
      setAuditLogs([]);
    } finally {
      setAuditLoading(false);
    }
  };

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-3xl bg-gradient-to-r from-indigo-950/60 via-slate-900 to-violet-950/60 border border-indigo-500/30">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-2xl font-black text-white tracking-tight">
              SaaS Admin Oversight Portal
            </h2>
            <p className="text-sm text-slate-400 mt-1">
              Cross-microservice management across users, subscriptions, and payment ledgers.
            </p>
          </div>
        </div>
        <Badge variant="info">Platform Root Access</Badge>
      </div>

      <ErrorAlert message={error} onDismiss={() => setError('')} />

      {/* Tabs */}
      <div className="flex items-center gap-3 border-b border-slate-800 pb-4">
        <button
          onClick={() => setActiveTab('users')}
          className={clsx(
            'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider transition-all',
            activeTab === 'users'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
              : 'bg-slate-900/60 text-slate-400 hover:text-white'
          )}
        >
          <Users className="w-4 h-4" />
          <span>All Users ({users.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('subscriptions')}
          className={clsx(
            'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider transition-all',
            activeTab === 'subscriptions'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
              : 'bg-slate-900/60 text-slate-400 hover:text-white'
          )}
        >
          <Layers className="w-4 h-4" />
          <span>All Subscriptions ({subscriptions.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('payments')}
          className={clsx(
            'flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider transition-all',
            activeTab === 'payments'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
              : 'bg-slate-900/60 text-slate-400 hover:text-white'
          )}
        >
          <CreditCard className="w-4 h-4" />
          <span>All Payments ({payments.length})</span>
        </button>
      </div>

      {/* Content Area */}
      {loading ? (
        <div className="flex h-64 items-center justify-center">
          <Loader size="lg" label="Loading administrative records..." />
        </div>
      ) : activeTab === 'users' ? (
        <div className="rounded-3xl border border-slate-800/80 bg-slate-900/60 backdrop-blur-md overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-[11px] font-bold uppercase tracking-widest text-slate-400">
                  <th className="px-6 py-4">User ID</th>
                  <th className="px-6 py-4">Full Name</th>
                  <th className="px-6 py-4">Email Address</th>
                  <th className="px-6 py-4">Roles</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-sm">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-800/40">
                    <td className="px-6 py-4 font-mono text-xs text-indigo-400 font-bold">
                      #{u.id}
                    </td>
                    <td className="px-6 py-4 font-bold text-white">
                      {u.fullName}
                    </td>
                    <td className="px-6 py-4 text-slate-300">{u.email}</td>
                    <td className="px-6 py-4">
                      <div className="flex flex-wrap gap-1">
                        {u.roles?.map((role) => (
                          <Badge key={role} variant="info">
                            {role}
                          </Badge>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : activeTab === 'subscriptions' ? (
        <div className="rounded-3xl border border-slate-800/80 bg-slate-900/60 backdrop-blur-md overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-[11px] font-bold uppercase tracking-widest text-slate-400">
                  <th className="px-6 py-4">ID</th>
                  <th className="px-6 py-4">Subscriber Email</th>
                  <th className="px-6 py-4">Plan</th>
                  <th className="px-6 py-4">Price</th>
                  <th className="px-6 py-4">Cycle</th>
                  <th className="px-6 py-4">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-sm">
                {subscriptions.map((sub) => (
                  <tr key={sub.id} className="hover:bg-slate-800/40">
                    <td className="px-6 py-4 font-mono text-xs text-indigo-400 font-bold">
                      #{sub.id}
                    </td>
                    <td className="px-6 py-4 text-slate-300">{sub.userEmail}</td>
                    <td className="px-6 py-4 font-bold text-white">
                      {sub.plan}
                    </td>
                    <td className="px-6 py-4 font-extrabold text-white">
                      ₹{sub.price}
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-400">
                      {sub.billingCycle}
                    </td>
                    <td className="px-6 py-4">
                      <Badge
                        variant={
                          sub.status === 'ACTIVE'
                            ? 'success'
                            : sub.status === 'CANCELLED'
                            ? 'danger'
                            : 'warning'
                        }
                      >
                        {sub.status}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        <div className="rounded-3xl border border-slate-800/80 bg-slate-900/60 backdrop-blur-md overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-[11px] font-bold uppercase tracking-widest text-slate-400">
                  <th className="px-6 py-4">ID</th>
                  <th className="px-6 py-4">Reference</th>
                  <th className="px-6 py-4">User #</th>
                  <th className="px-6 py-4">Sub #</th>
                  <th className="px-6 py-4">Amount</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4 text-right">Audit Trail</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-sm">
                {payments.map((pay) => (
                  <tr key={pay.id} className="hover:bg-slate-800/40">
                    <td className="px-6 py-4 font-mono text-xs text-indigo-400 font-bold">
                      #{pay.id}
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-200">
                      {pay.paymentReference}
                    </td>
                    <td className="px-6 py-4 text-slate-400">#{pay.userId}</td>
                    <td className="px-6 py-4 text-slate-400">
                      #{pay.subscriptionId}
                    </td>
                    <td className="px-6 py-4 font-extrabold text-white">
                      ₹{pay.amount} {pay.currency}
                    </td>
                    <td className="px-6 py-4">
                      <Badge
                        variant={
                          pay.status === 'SUCCESS' || pay.status === 'CAPTURED'
                            ? 'success'
                            : pay.status === 'CREATED' ||
                              pay.status === 'AUTHORIZED'
                            ? 'info'
                            : pay.status === 'PENDING'
                            ? 'warning'
                            : 'danger'
                        }
                      >
                        {pay.status}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Button
                        size="sm"
                        variant="secondary"
                        leftIcon={<History className="w-3.5 h-3.5" />}
                        onClick={() => handleOpenAuditModal(pay)}
                      >
                        Audit Trail
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Immutable Payment Audit Trail Modal */}
      <Modal
        isOpen={!!selectedPaymentForAudit}
        onClose={() => setSelectedPaymentForAudit(null)}
        title="Payment Immutable Audit Trail"
        subtitle={`State transitions for Payment #${selectedPaymentForAudit?.id} (${selectedPaymentForAudit?.paymentReference})`}
        size="lg"
      >
        {auditLoading ? (
          <div className="flex h-48 items-center justify-center">
            <Loader size="md" label="Querying payment_audit_logs..." />
          </div>
        ) : auditLogs.length === 0 ? (
          <div className="p-8 text-center text-slate-400">
            <p>No audit transition logs recorded for this payment yet.</p>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="space-y-3">
              {auditLogs.map((log) => (
                <div
                  key={log.id}
                  className="p-4 rounded-2xl bg-slate-950/80 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      {log.fromStatus && (
                        <>
                          <Badge variant="default">{log.fromStatus}</Badge>
                          <span className="text-slate-500 font-bold">→</span>
                        </>
                      )}
                      <Badge variant="info">{log.toStatus}</Badge>
                      <span className="text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">
                        {log.triggeredBy}
                      </span>
                    </div>
                    {log.reason && (
                      <p className="text-xs text-slate-300 mt-1">
                        {log.reason}
                      </p>
                    )}
                  </div>
                  <span className="text-xs text-slate-500 font-mono shrink-0">
                    {new Date(log.timestamp).toLocaleString('en-IN', {
                      dateStyle: 'medium',
                      timeStyle: 'medium',
                    })}
                  </span>
                </div>
              ))}
            </div>

            <div className="flex justify-end pt-2">
              <Button
                variant="secondary"
                onClick={() => setSelectedPaymentForAudit(null)}
              >
                Close
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
