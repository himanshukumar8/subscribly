import axiosClient from './axiosClient';
import type { AuthResponse, User } from '../types';

export const authApi = {
  login: async (email: string, password: string): Promise<AuthResponse> => {
    const res = await axiosClient.post('/auth/login', { email, password });
    return res.data;
  },

  register: async (email: string, password: string, fullName: string): Promise<User> => {
    const res = await axiosClient.post('/auth/register', { email, password, fullName });
    return res.data;
  },

  getMe: async (): Promise<User> => {
    const res = await axiosClient.get('/user/me');
    return res.data;
  },

  adminGetAllUsers: async (): Promise<User[]> => {
    const res = await axiosClient.get('/admin/users/all');
    return res.data;
  },
};
