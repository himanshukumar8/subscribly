import axiosClient from './axiosClient';
import type { Subscription, BillingCycle } from '../types';

export interface CreateSubscriptionRequest {
  plan: string;
  price: number;
  billingCycle: BillingCycle;
}

export const subscriptionApi = {
  getMySubscriptions: async (): Promise<Subscription[]> => {
    const res = await axiosClient.get('/subscription/my');
    return res.data;
  },

  createSubscription: async (data: CreateSubscriptionRequest): Promise<Subscription> => {
    const res = await axiosClient.post('/subscription', data);
    return res.data;
  },

  cancelSubscription: async (id: number): Promise<Subscription> => {
    const res = await axiosClient.put(`/subscription/${id}/cancel`);
    return res.data;
  },

  adminGetAllSubscriptions: async (): Promise<Subscription[]> => {
    const res = await axiosClient.get('/admin/subscriptions/all');
    return res.data;
  },
};
