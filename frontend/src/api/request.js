import axios from 'axios';
import { ElMessage } from 'element-plus';
import { createDeviceHeaders, getOrCreateDeviceId, isDeviceMissingError } from '../utils/deviceId';

const request = axios.create({
  baseURL: '/api',
  timeout: 60000
});

request.interceptors.request.use((config) => {
  config.headers = createDeviceHeaders(config.headers || {});
  return config;
});

request.interceptors.response.use(
  (response) => {
    const result = response.data;
    if (result.code !== 200) {
      if (isDeviceMissingError(new Error(result.message)) && !response.config?._deviceRetry) {
        getOrCreateDeviceId();
        return request({
          ...response.config,
          _deviceRetry: true,
          headers: createDeviceHeaders(response.config?.headers || {})
        });
      }
      if (!response.config?.silentError) {
        ElMessage.error(result.message || '请求失败');
      }
      return Promise.reject(new Error(result.message || '请求失败'));
    }
    return result.data;
  },
  (error) => {
    if (isDeviceMissingError(error) && !error.config?._deviceRetry) {
      getOrCreateDeviceId();
      return request({
        ...error.config,
        _deviceRetry: true,
        headers: createDeviceHeaders(error.config?.headers || {})
      });
    }
    const isProxyBackendError = error.response?.status === 500
      && typeof error.response?.data === 'string'
      && error.response.data.includes('http://localhost:8080');
    const message = isProxyBackendError
      ? '后端服务未启动或不可访问，请先启动 Spring Boot 服务'
      : error.response?.data?.message || error.message || '网络异常';
    if (!error.config?.silentError) {
      ElMessage.error(message);
    }
    return Promise.reject(error);
  }
);

export default request;
