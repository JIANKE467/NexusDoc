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
    ElMessage.error(error.response?.data?.message || error.message || '网络异常');
    return Promise.reject(error);
  }
);

export default request;
