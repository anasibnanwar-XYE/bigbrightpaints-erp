/**
 * BigBright ERP API Client Configuration
 * 
 * This file shows how to configure the API client for different environments.
 * Copy this to your frontend project and adjust as needed.
 */

import axios, { AxiosInstance } from 'axios';
import { Configuration, AuthControllerApi, SalesControllerApi, AccountingControllerApi } from './index';

// Environment-based API URL configuration
const getApiBaseUrl = (): string => {
  // For Vite
  if (typeof import.meta !== 'undefined' && (import.meta as any).env?.VITE_API_URL) {
    return (import.meta as any).env.VITE_API_URL;
  }
  
  // For Create React App
  if (typeof process !== 'undefined' && process.env?.REACT_APP_API_URL) {
    return process.env.REACT_APP_API_URL;
  }
  
  // For Next.js
  if (typeof process !== 'undefined' && process.env?.NEXT_PUBLIC_API_URL) {
    return process.env.NEXT_PUBLIC_API_URL;
  }
  
  // Runtime config (for Docker/K8s deployments)
  if (typeof window !== 'undefined' && (window as any).ENV?.API_URL) {
    return (window as any).ENV.API_URL;
  }
  
  // Default fallback
  return 'http://localhost:8081';
};

// Token storage
let accessToken: string | null = null;
let refreshToken: string | null = null;

export const setTokens = (access: string, refresh: string) => {
  accessToken = access;
  refreshToken = refresh;
  localStorage.setItem('accessToken', access);
  localStorage.setItem('refreshToken', refresh);
};

export const clearTokens = () => {
  accessToken = null;
  refreshToken = null;
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
};

export const loadTokens = () => {
  accessToken = localStorage.getItem('accessToken');
  refreshToken = localStorage.getItem('refreshToken');
};

// Create axios instance with interceptors
const createAxiosInstance = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: getApiBaseUrl(),
    headers: {
      'Content-Type': 'application/json',
    },
  });

  // Request interceptor - add auth token
  instance.interceptors.request.use(
    (config) => {
      if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
      }
      // Add company context header if set
      const companyCode = localStorage.getItem('companyCode');
      if (companyCode) {
        config.headers['X-Company-Id'] = companyCode;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response interceptor - handle token refresh
  instance.interceptors.response.use(
    (response) => response,
    async (error) => {
      const originalRequest = error.config;
      
      if (error.response?.status === 401 && !originalRequest._retry && refreshToken) {
        originalRequest._retry = true;
        
        try {
          const response = await axios.post(`${getApiBaseUrl()}/api/v1/auth/refresh`, {
            refreshToken,
          });
          
          const { accessToken: newAccess, refreshToken: newRefresh } = response.data;
          setTokens(newAccess, newRefresh);
          
          originalRequest.headers.Authorization = `Bearer ${newAccess}`;
          return instance(originalRequest);
        } catch (refreshError) {
          clearTokens();
          window.location.href = '/login';
          return Promise.reject(refreshError);
        }
      }
      
      return Promise.reject(error);
    }
  );

  return instance;
};

// API Configuration
const axiosInstance = createAxiosInstance();

const apiConfig = new Configuration({
  basePath: getApiBaseUrl(),
  accessToken: () => accessToken || '',
});

// Export configured API clients
export const authApi = new AuthControllerApi(apiConfig, getApiBaseUrl(), axiosInstance);
export const salesApi = new SalesControllerApi(apiConfig, getApiBaseUrl(), axiosInstance);
export const accountingApi = new AccountingControllerApi(apiConfig, getApiBaseUrl(), axiosInstance);

// Example usage:
/*
import { authApi, salesApi, setTokens, loadTokens } from './api-config';

// On app start
loadTokens();

// Login
const login = async (email: string, password: string) => {
  const response = await authApi.login({ email, password });
  setTokens(response.data.accessToken, response.data.refreshToken);
  localStorage.setItem('companyCode', response.data.user.companies[0].code);
};

// Get sales orders
const getOrders = async () => {
  const response = await salesApi.listOrders();
  return response.data.data;
};
*/

export { getApiBaseUrl };
