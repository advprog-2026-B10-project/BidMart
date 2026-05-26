const isProd = process.env.NODE_ENV === 'production';
const API_BASE_URL = isProd ? '' : 'http://localhost:8080';
export const API_URL = API_BASE_URL;
export const API_URL_WITH_API = `${API_BASE_URL}/api`;