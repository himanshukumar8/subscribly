import React, { useEffect, useState } from 'react';
import { paymentApi } from '../../api/paymentApi';
import { subscriptionApi } from '../../api/subscriptionApi';
import type { Payment, Subscription } from '../../types';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Loader } from '../../components/common/Loader';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { Modal } from '../../components/common/Modal';
import {
  PlusCircle,
  Receipt,
} from 'lucide-react';
import clsx from 'clsx';

export const PaymentsPage: React.FC = () => {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState<string>('ALL');

  // Modal states
  const [selectedPayment, setSelectedPayment] = useState<Payment | null>(null);
  const [isOrderModalOpen, setIsOrderModalOpen] = useState(false);
  const [selectedSubId, setSelectedSubId] = useState<number | ''>('');
  const [orderAmount, setOrderAmount] = useState<number>(999);
  const [orderLoading, setOrderLoading] = useState(false);

  const fetchPaymentsData = async () => {
    setLoading(true);
    setError('');
    try {
      const [payData, subsData] = await Promise.all([
        paymentApi.getMyPayments(),
        subscriptionApi.getMySubscriptions(),
      ]);
      setPayments(payData);
      setSubscriptions(subsData);
      if (subsData.length > 0) {
        setSelectedSubId(subsData[0].id);
        setOrderAmount(subsData[0].price);
      }
    } catch (err: any) {
      console.error('Error loading payments:', err);
      setError('Failed to load payment transactions.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPaymentsData();
  }, []);

  const handleCreateOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSubId) return;
    setOrderLoading(true);
    setError('');
    try {
      const newOrder = await paymentApi.createPaymentOrder({
        subscriptionId: Number(selectedSubId),
        amount: orderAmount,
        currency: 'INR',
      });
      setPayments((prev) => [newOrder, ...prev]);
      setIsOrderModalOpen(false);
    } catch (err: any) {
      console.error('Order creation error:', err);
      setError(
        err.response?.data?.message ||
          'Failed to initiate payment order. Please try again.'
      );
    } finally {
      setOrderLoading(false);
    }
  };

  const filteredPayments = payments.filter((p) => {
    if (filter === 'ALL') return true;
    return p.status === filter;
  });

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-black text-white tracking-tight">
            Payments & Billing
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Complete transaction history and Razorpay payment order state machine.
          </p>
        </div>
        <Button
          variant="primary"
          size="md"
          onClick={() => setIsOrderModalOpen(true)}
          leftIcon={<PlusCircle className="w-4 h-4" />}
          disabled={subscriptions.length === 0}
        >
          Initiate Payment Order
        </Button>
      </div>

      <ErrorAlert message={error} onDismiss={() => setError('')} />

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2">
        {['ALL', 'SUCCESS', 'CREATED', 'PENDING', 'FAILED', 'REFUNDED'].map(
          (status) => (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={clsx(
                'px-4 py-2 rounded-xl text-xs font-bold uppercase tracking-wider transition-all',
                filter === status
                  ? 'bg-indigo-500/20 text-indigo-400 border border-indigo-500/30'
                  : 'bg-slate-900/60 text-slate-400 border border-slate-800 hover:text-slate-200'
              )}
            >
              {status} (
                {status === 'ALL'
                  ? payments.length
                  : payments.filter((p) => p.status === status).length}
                )
            </button>
          )
        )}
      </div>

      {/* Payment Table / Card List */}
      {loading ? (
        <div className="flex h-64 items-center justify-center">
          <Loader size="md" label="Loading transaction ledger..." />
        </div>
      ) : filteredPayments.length === 0 ? (
        <EmptyState
          icon={Receipt}
          title="No transactions found"
          description={
            filter === 'ALL'
              ? 'No payment orders have been generated yet.'
              : `No payments with status "${filter}".`
          }
          actionLabel={
            filter === 'ALL' && subscriptions.length > 0
              ? 'Create First Order'
              : undefined
          }
          onAction={() => setIsOrderModalOpen(true)}
        />
      ) : (
        <div className="rounded-3xl border border-slate-800/80 bg-slate-900/60 backdrop-blur-md overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-[11px] font-bold uppercase tracking-widest text-slate-400">
                  <th className="px-6 py-4">Reference</th>
                  <th className="px-6 py-4">Gateway Order ID</th>
                  <th className="px-6 py-4">Subscription #</th>
                  <th className="px-6 py-4">Amount</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Created Date</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-sm">
                {filteredPayments.map((pay) => (
                  <tr
                    key={pay.id}
                    className="hover:bg-slate-800/40 transition-colors"
                  >
                    <td className="px-6 py-4 font-mono text-xs font-semibold text-slate-200">
                      {pay.paymentReference}
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-400">
                      {pay.gatewayOrderId || 'Pending'}
                    </td>
                    <td className="px-6 py-4 font-semibold text-slate-300">
                      #{pay.subscriptionId}
                    </td>
                    <td className="px-6 py-4 font-extrabold text-white">
                      ₹{pay.amount.toLocaleString('en-IN')}{' '}
                      <span className="text-[10px] font-normal text-slate-400">
                        {pay.currency}
                      </span>
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
                    <td className="px-6 py-4 text-xs text-slate-400">
                      {new Date(pay.createdAt).toLocaleString('en-IN', {
                        dateStyle: 'medium',
                        timeStyle: 'short',
                      })}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => setSelectedPayment(pay)}
                      >
                        Details
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Payment Details Modal */}
      <Modal
        isOpen={!!selectedPayment}
        onClose={() => setSelectedPayment(null)}
        title="Payment Transaction Details"
        subtitle={`Ledger inspection for ${selectedPayment?.paymentReference}`}
        size="md"
      >
        {selectedPayment && (
          <div className="space-y-4 text-sm">
            <div className="p-4 rounded-2xl bg-slate-950/80 border border-slate-800 space-y-3">
              <div className="flex justify-between">
                <span className="text-slate-400 font-medium">Status</span>
                <Badge
                  variant={
                    selectedPayment.status === 'SUCCESS'
                      ? 'success'
                      : selectedPayment.status === 'CREATED'
                      ? 'info'
                      : 'warning'
                  }
                >
                  {selectedPayment.status}
                </Badge>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400 font-medium">Payment ID</span>
                <span className="font-mono text-xs text-slate-200">
                  #{selectedPayment.id}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400 font-medium">Reference</span>
                <span className="font-mono text-xs text-indigo-400">
                  {selectedPayment.paymentReference}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400 font-medium">
                  Gateway Order ID
                </span>
                <span className="font-mono text-xs text-slate-300">
                  {selectedPayment.gatewayOrderId || 'N/A'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400 font-medium">Amount</span>
                <span className="font-extrabold text-white text-base">
                  ₹{selectedPayment.amount} {selectedPayment.currency}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400 font-medium">Retry Count</span>
                <span className="font-semibold text-slate-200">
                  {selectedPayment.retryCount} attempt(s)
                </span>
              </div>
              {selectedPayment.failureReason && (
                <div className="pt-2 border-t border-slate-800">
                  <span className="text-rose-400 font-semibold block mb-1 text-xs uppercase">
                    Failure Reason
                  </span>
                  <p className="text-xs text-rose-300">
                    {selectedPayment.failureReason}
                  </p>
                </div>
              )}
            </div>

            <div className="flex justify-end">
              <Button
                variant="secondary"
                onClick={() => setSelectedPayment(null)}
              >
                Close
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Initiate Payment Order Modal */}
      <Modal
        isOpen={isOrderModalOpen}
        onClose={() => setIsOrderModalOpen(false)}
        title="Initiate Razorpay Payment Order"
        subtitle="Test order creation against payment-service (Port 8083)"
        size="md"
      >
        <form onSubmit={handleCreateOrder} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
              Select Subscription
            </label>
            <select
              value={selectedSubId}
              onChange={(e) => {
                const subId = Number(e.target.value);
                setSelectedSubId(subId);
                const found = subscriptions.find((s) => s.id === subId);
                if (found) setOrderAmount(found.price);
              }}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-indigo-500"
            >
              {subscriptions.map((sub) => (
                <option key={sub.id} value={sub.id}>
                  #{sub.id} - {sub.plan} Plan (₹{sub.price} / {sub.billingCycle})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
              Amount (INR)
            </label>
            <input
              type="number"
              required
              min={1}
              value={orderAmount}
              onChange={(e) => setOrderAmount(Number(e.target.value))}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-100 focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="p-3 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-xs text-indigo-300">
            This will call POST /api/v1/payment/order and return a gatewayOrderId signed with HMAC-SHA256.
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsOrderModalOpen(false)}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={orderLoading}>
              Create Payment Order
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
