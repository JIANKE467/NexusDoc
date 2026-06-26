import axios from 'axios';
import { ElMessage } from 'element-plus';

const request = axios.create({
  baseURL: '/api',
  timeout: 60000
});

request.interceptors.response.use(
  (response) => {
    const result = response.data;
    if (result.code !== 200) {
      ElMessage.error(result.message || '请求失败');
      return Promise.reject(new Error(result.message || '请求失败'));
    }
    return result.data;
  },
  (error) => {
    const isProxyBackendError = error.response?.status === 500
      && typeof error.response?.data === 'string'
      && error.response.data.includes('http://localhost:8080');
    const message = isProxyBackendError
      ? '后端服务未启动或不可访问，请先启动 Spring Boot 服务'
      : error.response?.data?.message || error.message || '网络异常';
    ElMessage.error(message);
    return Promise.reject(error);
  }
);

export default request;
