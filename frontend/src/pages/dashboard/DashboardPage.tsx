import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { subscriptionApi } from '../../api/subscriptionApi';
import { paymentApi } from '../../api/paymentApi';
import type { Subscription, Payment } from '../../types';
import { StatCard } from '../../components/common/StatCard';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { Loader } from '../../components/common/Loader';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import {
  Layers,
  CreditCard,
  Calendar,
  PlusCircle,
  ArrowUpRight,
  TrendingUp,
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchDashboardData = async () => {
    setLoading(true);
    setError('');
    try {
      const [subsData, payData] = await Promise.all([
        subscriptionApi.getMySubscriptions(),
        paymentApi.getMyPayments(),
      ]);
      setSubscriptions(subsData);
      setPayments(payData);
    } catch (err: any) {
      console.error('Error loading dashboard data:', err);
      setError(
        err.response?.data?.message ||
          'Failed to load dashboard metrics. Please refresh.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const activeSubsCount = subscriptions.filter(
    (s) => s.status === 'ACTIVE'
  ).length;

  const totalSpent = payments
    .filter((p) => p.status === 'SUCCESS' || p.status === 'CAPTURED')
    .reduce((acc, curr) => acc + curr.amount, 0);

  const nextBillingDate = subscriptions.find((s) => s.status === 'ACTIVE')
    ?.endDate || 'No upcoming renewal';

  if (loading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <Loader size="lg" label="Loading SaaS analytics..." />
      </div>
    );
  }

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Welcome Hero Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-6 rounded-3xl bg-gradient-to-r from-indigo-900/40 via-slate-900/80 to-violet-900/30 border border-slate-800/80">
        <div>
          <h2 className="text-2xl font-black text-white tracking-tight">
            Welcome back, {user?.fullName || 'Subscriber'}!
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Here is your microservice subscription & payment performance overview.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Link to="/subscriptions">
            <Button
              variant="primary"
              size="md"
              leftIcon={<PlusCircle className="w-4 h-4" />}
            >
              New Subscription
            </Button>
          </Link>
          <Link to="/payments">
            <Button variant="secondary" size="md">
              View Payments
            </Button>
          </Link>
        </div>
      </div>

      <ErrorAlert message={error} onDismiss={() => setError('')} />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <StatCard
          title="Active Subscriptions"
          value={activeSubsCount}
          subtitle={`${subscriptions.length} total subscriptions`}
          icon={Layers}
          accent="indigo"
          trend={{ value: '+1', isPositive: true }}
        />
        <StatCard
          title="Total Spent"
          value={`₹${totalSpent.toLocaleString('en-IN')}`}
          subtitle="Processed via Razorpay gateway"
          icon={CreditCard}
          accent="emerald"
          trend={{ value: '+12.5%', isPositive: true }}
        />
        <StatCard
          title="Next Renewal Date"
          value={
            typeof nextBillingDate === 'string' &&
            nextBillingDate !== 'No upcoming renewal'
              ? new Date(nextBillingDate).toLocaleDateString('en-IN', {
                  day: 'numeric',
                  month: 'short',
                })
              : 'N/A'
          }
          subtitle={
            typeof nextBillingDate === 'string' &&
            nextBillingDate !== 'No upcoming renewal'
              ? nextBillingDate
              : 'No upcoming renewal'
          }
          icon={Calendar}
          accent="violet"
        />
      </div>

      {/* Recent Subscriptions & Payments Split Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Active Subscriptions Card */}
        <div className="p-6 rounded-3xl bg-slate-900/60 border border-slate-800/80">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-lg font-bold text-white">
                Your Subscriptions
              </h3>
              <p className="text-xs text-slate-400">
                Managed by subscription-service (Port 8082)
              </p>
            </div>
            <Link
              to="/subscriptions"
              className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 flex items-center gap-1"
            >
              <span>View All</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {subscriptions.length === 0 ? (
            <div className="p-8 text-center border border-dashed border-slate-800 rounded-2xl">
              <p className="text-sm text-slate-400 mb-3">
                No active plans found.
              </p>
              <Link to="/subscriptions">
                <Button size="sm" variant="primary">
                  Explore Plans
                </Button>
              </Link>
            </div>
          ) : (
            <div className="space-y-3">
              {subscriptions.slice(0, 4).map((sub) => (
                <div
                  key={sub.id}
                  className="flex items-center justify-between p-4 rounded-2xl bg-slate-950/60 border border-slate-800/80 hover:border-slate-700/80 transition-colors"
                >
                  <div className="flex items-center gap-3.5">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-500/10 text-indigo-400 font-bold border border-indigo-500/20">
                      {sub.plan.slice(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-white">
                        {sub.plan} Plan
                      </h4>
                      <p className="text-xs text-slate-400">
                        ₹{sub.price} • {sub.billingCycle}
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
              ))}
            </div>
          )}
        </div>

        {/* Recent Payment Activity */}
        <div className="p-6 rounded-3xl bg-slate-900/60 border border-slate-800/80">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-lg font-bold text-white">
                Recent Payment Orders
              </h3>
              <p className="text-xs text-slate-400">
                Managed by payment-service (Port 8083)
              </p>
            </div>
            <Link
              to="/payments"
              className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 flex items-center gap-1"
            >
              <span>View History</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {payments.length === 0 ? (
            <div className="p-8 text-center border border-dashed border-slate-800 rounded-2xl">
              <p className="text-sm text-slate-400">
                No payment transactions yet.
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {payments.slice(0, 4).map((pay) => (
                <div
                  key={pay.id}
                  className="flex items-center justify-between p-4 rounded-2xl bg-slate-950/60 border border-slate-800/80"
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-mono font-semibold text-slate-300">
                        {pay.paymentReference}
                      </span>
                      {pay.gatewayOrderId && (
                        <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-400">
                          {pay.gatewayOrderId}
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-slate-500 mt-1">
                      {new Date(pay.createdAt).toLocaleString('en-IN', {
                        dateStyle: 'medium',
                        timeStyle: 'short',
                      })}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-sm font-black text-white">
                      ₹{pay.amount}
                    </span>
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
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* System Health / Event Architecture Banner */}
      <div className="p-6 rounded-3xl bg-gradient-to-r from-slate-900 to-slate-900/60 border border-slate-800/80 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <h4 className="text-base font-bold text-white">
              Event-Driven Platform Active
            </h4>
            <p className="text-xs text-slate-400">
              API Gateway (8080) • Discovery (8761) • Config Server (8888) • Kafka Events
            </p>
          </div>
        </div>
        <Badge variant="success">All Services Online</Badge>
      </div>
    </div>
  );
};
