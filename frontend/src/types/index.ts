export interface User {
  id: number;
  email: string;
  fullName: string;
  roles?: string[];
}

export type SubscriptionStatus = 'ACTIVE' | 'CANCELLED' | 'EXPIRED' | 'PAST_DUE';
export type BillingCycle = 'MONTHLY' | 'YEARLY';

export interface Subscription {
  id: number;
  userId: number;
  userEmail: string;
  plan: string;
  price: number;
  billingCycle: BillingCycle;
  status: SubscriptionStatus;
  startDate?: string;
  endDate?: string;
}

export type PaymentStatus =
  | 'CREATED'
  | 'PENDING'
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'SUCCESS'
  | 'FAILED'
  | 'REFUNDED'
  | 'CANCELLED';

export interface Payment {
  id: number;
  paymentReference: string;
  gatewayOrderId?: string;
  gatewayPaymentId?: string;
  subscriptionId: number;
  userId: number;
  amount: number;
  currency: string;
  status: PaymentStatus;
  retryCount: number;
  nextRetryAt?: string;
  failureReason?: string;
  createdAt: string;
}

export interface PaymentAuditLog {
  id: number;
  paymentId: number;
  paymentReference: string;
  fromStatus?: PaymentStatus;
  toStatus: PaymentStatus;
  reason?: string;
  triggeredBy: 'USER' | 'WEBHOOK' | 'SYSTEM_RETRY' | 'ADMIN';
  timestamp: string;
}

export interface AuthResponse {
  token: string;
  user?: User;
}
