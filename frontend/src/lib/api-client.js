import axios from 'axios';

// Single axios instance for the whole app. Base URL is relative so the Vite
// dev proxy (and the production reverse proxy) route /api to the backend.
// JWT attach + refresh interceptors will be wired here when the Auth module lands.
export const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error)
);
