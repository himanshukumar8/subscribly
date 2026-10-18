import axiosClient from './axiosClient';
import type { Payment, PaymentAuditLog } from '../types';

export interface CreatePaymentOrderRequest {
  subscriptionId: number;
  amount: number;
  currency?: string;
}

export interface VerifyPaymentRequest {
  orderId: string;
  paymentId: string;
  signature: string;
}

export const paymentApi = {
  getMyPayments: async (): Promise<Payment[]> => {
    const res = await axiosClient.get('/payment/my');
    return res.data;
  },

  createPaymentOrder: async (data: CreatePaymentOrderRequest): Promise<Payment> => {
    const res = await axiosClient.post('/payment/order', data);
    return res.data;
  },

  verifyPayment: async (data: VerifyPaymentRequest): Promise<Payment> => {
    const res = await axiosClient.post('/payment/verify', data);
    return res.data;
  },

  adminGetAllPayments: async (): Promise<Payment[]> => {
    const res = await axiosClient.get('/admin/payments/all');
    return res.data;
  },

  adminGetPaymentAuditLogs: async (paymentId: number): Promise<PaymentAuditLog[]> => {
    const res = await axiosClient.get(`/admin/payments/${paymentId}/audit-logs`);
    return res.data;
  },
};
