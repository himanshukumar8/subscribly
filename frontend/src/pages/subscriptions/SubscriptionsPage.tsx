import React, { useEffect, useState } from 'react';
import { subscriptionApi } from '../../api/subscriptionApi';
import type { Subscription, BillingCycle } from '../../types';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Loader } from '../../components/common/Loader';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { Modal } from '../../components/common/Modal';
import {
  Layers,
  PlusCircle,
  CheckCircle2,
  Zap,
  Sparkles,
  Shield,
  Calendar,
} from 'lucide-react';
import clsx from 'clsx';

export const SubscriptionsPage: React.FC = () => {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState<string>('ALL');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedCycle, setSelectedCycle] = useState<BillingCycle>('MONTHLY');
  const [subscribingPlan, setSubscribingPlan] = useState<string | null>(null);

  const fetchSubscriptions = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await subscriptionApi.getMySubscriptions();
      setSubscriptions(data);
    } catch (err: any) {
      console.error('Error loading subscriptions:', err);
      setError('Failed to fetch subscription records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubscriptions();
  }, []);

  const handleSubscribe = async (planName: string, price: number) => {
    setSubscribingPlan(planName);
    setError('');
    try {
      const newSub = await subscriptionApi.createSubscription({
        plan: planName,
        price,
        billingCycle: selectedCycle,
      });
      setSubscriptions((prev) => [newSub, ...prev]);
      setIsModalOpen(false);
    } catch (err: any) {
      console.error('Subscription creation error:', err);
      setError(
        err.response?.data?.message ||
          'Failed to create subscription plan. Please try again.'
      );
    } finally {
      setSubscribingPlan(null);
    }
  };

  const handleCancelSubscription = async (id: number) => {
    if (!window.confirm('Are you sure you want to cancel this subscription?')) {
      return;
    }
    setError('');
    try {
      const updated = await subscriptionApi.cancelSubscription(id);
      setSubscriptions((prev) =>
        prev.map((sub) => (sub.id === id ? updated : sub))
      );
    } catch (err: any) {
      console.error('Cancel error:', err);
      setError('Could not cancel subscription. It may already be cancelled.');
    }
  };

  const filteredSubs = subscriptions.filter((s) => {
    if (filter === 'ALL') return true;
    return s.status === filter;
  });

  const pricingPlans = [
    {
      name: 'Starter',
      monthlyPrice: 499,
      yearlyPrice: 4990,
      icon: Zap,
      badge: 'Entry',
      features: [
        '1 User Account',
        'Basic Dashboard Metrics',
        'Standard Email Notification',
        '24/7 Community Support',
      ],
    },
    {
      name: 'Pro',
      monthlyPrice: 999,
      yearlyPrice: 9990,
      icon: Sparkles,
      badge: 'Most Popular',
      isPopular: true,
      features: [
        'Up to 5 User Accounts',
        'Full Microservice Analytics',
        'Instant Razorpay Payment Verification',
        'Priority SLA Support',
      ],
    },
    {
      name: 'Enterprise',
      monthlyPrice: 2499,
      yearlyPrice: 24990,
      icon: Shield,
      badge: 'Scale',
      features: [
        'Unlimited User Accounts',
        'Custom Kafka Outbox Integrations',
        'Dedicated Technical Account Manager',
        'Audit Trail & Export Logs',
      ],
    },
  ];

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-black text-white tracking-tight">
            Subscriptions
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Manage your active SaaS plans and billing renewals.
          </p>
        </div>
        <Button
          variant="primary"
          size="md"
          onClick={() => setIsModalOpen(true)}
          leftIcon={<PlusCircle className="w-4 h-4" />}
        >
          Add New Subscription
        </Button>
      </div>

      <ErrorAlert message={error} onDismiss={() => setError('')} />

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2">
        {['ALL', 'ACTIVE', 'CANCELLED', 'EXPIRED'].map((status) => (
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
                ? subscriptions.length
                : subscriptions.filter((s) => s.status === status).length}
            )
          </button>
        ))}
      </div>

      {/* Subscriptions List */}
      {loading ? (
        <div className="flex h-64 items-center justify-center">
          <Loader size="md" label="Loading subscriptions from database..." />
        </div>
      ) : filteredSubs.length === 0 ? (
        <EmptyState
          icon={Layers}
          title="No subscriptions found"
          description={
            filter === 'ALL'
              ? 'You have not subscribed to any plan yet. Choose a plan to get started.'
              : `No subscriptions with status "${filter}".`
          }
          actionLabel={filter === 'ALL' ? 'Choose a Plan' : undefined}
          onAction={() => setIsModalOpen(true)}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {filteredSubs.map((sub) => (
            <div
              key={sub.id}
              className="p-6 rounded-3xl bg-slate-900/70 border border-slate-800/80 backdrop-blur-md hover:border-slate-700 transition-all flex flex-col justify-between"
            >
              <div>
                <div className="flex items-start justify-between gap-4 mb-4">
                  <div className="flex items-center gap-3">
                    <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-500/20 to-violet-500/10 border border-indigo-500/30 text-indigo-400 font-extrabold text-lg">
                      {sub.plan.slice(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <h3 className="text-lg font-bold text-white">
                        {sub.plan} Plan
                      </h3>
                      <p className="text-xs text-slate-400">
                        Subscription ID: #{sub.id}
                      </p>
                    </div>
                  </div>
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
                </div>

                <div className="grid grid-cols-2 gap-4 py-4 border-y border-slate-800/80 my-4 text-xs">
                  <div>
                    <span className="text-slate-500 font-semibold uppercase block">
                      Price / Cycle
                    </span>
                    <span className="text-base font-extrabold text-white">
                      ₹{sub.price}
                    </span>
                    <span className="text-slate-400 ml-1">
                      / {sub.billingCycle.toLowerCase()}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-500 font-semibold uppercase block">
                      Next Billing Date
                    </span>
                    <span className="text-sm font-medium text-slate-300">
                      {sub.endDate
                        ? new Date(sub.endDate).toLocaleDateString('en-IN', {
                            dateStyle: 'medium',
                          })
                        : 'N/A'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-between pt-2">
                <div className="flex items-center gap-1.5 text-xs text-slate-400">
                  <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  <span>
                    Started{' '}
                    {sub.startDate
                      ? new Date(sub.startDate).toLocaleDateString('en-IN')
                      : 'Recently'}
                  </span>
                </div>

                {sub.status === 'ACTIVE' && (
                  <Button
                    size="sm"
                    variant="danger"
                    onClick={() => handleCancelSubscription(sub.id)}
                  >
                    Cancel Plan
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Subscription Pricing Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Choose a Subscribly Plan"
        subtitle="Select a billing tier to activate SaaS features"
        size="xl"
      >
        <div className="space-y-6">
          {/* Billing Cycle Toggle */}
          <div className="flex items-center justify-center">
            <div className="inline-flex p-1 rounded-2xl bg-slate-950 border border-slate-800">
              <button
                type="button"
                onClick={() => setSelectedCycle('MONTHLY')}
                className={clsx(
                  'px-6 py-2 rounded-xl text-xs font-bold transition-all',
                  selectedCycle === 'MONTHLY'
                    ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
                    : 'text-slate-400 hover:text-white'
                )}
              >
                Monthly Billing
              </button>
              <button
                type="button"
                onClick={() => setSelectedCycle('YEARLY')}
                className={clsx(
                  'px-6 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5',
                  selectedCycle === 'YEARLY'
                    ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25'
                    : 'text-slate-400 hover:text-white'
                )}
              >
                <span>Yearly Billing</span>
                <span className="px-1.5 py-0.5 rounded-md bg-emerald-500/20 text-emerald-400 text-[10px]">
                  Save 15%
                </span>
              </button>
            </div>
          </div>

          {/* Pricing Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {pricingPlans.map((plan) => {
              const Icon = plan.icon;
              const price =
                selectedCycle === 'MONTHLY'
                  ? plan.monthlyPrice
                  : plan.yearlyPrice;

              return (
                <div
                  key={plan.name}
                  className={clsx(
                    'relative p-6 rounded-3xl border flex flex-col justify-between transition-all duration-300',
                    plan.isPopular
                      ? 'bg-gradient-to-b from-indigo-950/40 to-slate-900 border-indigo-500/50 shadow-xl shadow-indigo-500/10'
                      : 'bg-slate-950/60 border-slate-800/80 hover:border-slate-700'
                  )}
                >
                  {plan.isPopular && (
                    <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full bg-gradient-to-r from-indigo-500 to-violet-600 text-white text-[10px] font-black uppercase tracking-wider shadow-md">
                      Most Popular
                    </div>
                  )}

                  <div>
                    <div className="flex items-center justify-between mb-4">
                      <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                        {plan.badge}
                      </span>
                      <div className="p-2 rounded-xl bg-slate-800/80 text-indigo-400">
                        <Icon className="w-5 h-5" />
                      </div>
                    </div>

                    <h4 className="text-xl font-extrabold text-white mb-2">
                      {plan.name}
                    </h4>

                    <div className="flex items-baseline gap-1 mb-6">
                      <span className="text-3xl font-black text-white">
                        ₹{price.toLocaleString('en-IN')}
                      </span>
                      <span className="text-xs text-slate-400">
                        / {selectedCycle.toLowerCase()}
                      </span>
                    </div>

                    <ul className="space-y-3 mb-6 text-xs text-slate-300">
                      {plan.features.map((feat) => (
                        <li key={feat} className="flex items-center gap-2.5">
                          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                          <span>{feat}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  <Button
                    variant={plan.isPopular ? 'primary' : 'secondary'}
                    size="md"
                    className="w-full"
                    isLoading={subscribingPlan === plan.name}
                    onClick={() => handleSubscribe(plan.name, price)}
                  >
                    Subscribe to {plan.name}
                  </Button>
                </div>
              );
            })}
          </div>
        </div>
      </Modal>
    </div>
  );
};
