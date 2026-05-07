import axiosClient from './axiosClient';

export const logout = async () => {
  try {
    await axiosClient.post('/auth/logout');
  } catch (error) {
    console.error('Logout error:', error);
  } finally {
    // Always clear local storage and redirect
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('email');
    localStorage.removeItem('role');
    window.location.href = '/login';
  }
};

export const isAuthenticated = (): boolean => {
  if (typeof window === 'undefined') return false;
  return !!localStorage.getItem('token');
};

export const getCurrentUser = () => {
  if (typeof window === 'undefined') return null;
  const email = localStorage.getItem('email');
  const role = localStorage.getItem('role');
  return email ? { email, role } : null;
};
