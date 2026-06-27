const DEVICE_ID_KEY = 'NEXUSDOC_DEVICE_ID';
const DEVICE_ID_COOKIE = 'NEXUSDOC_DEVICE_ID';
let memoryDeviceId = '';

export function getOrCreateDeviceId() {
  if (memoryDeviceId) {
    return memoryDeviceId;
  }

  let deviceId = safeLocalStorageGet(DEVICE_ID_KEY) || readCookie(DEVICE_ID_COOKIE);
  if (!deviceId) {
    deviceId = generateDeviceId();
  }

  memoryDeviceId = deviceId;
  safeLocalStorageSet(DEVICE_ID_KEY, deviceId);
  writeCookie(DEVICE_ID_COOKIE, deviceId);
  return deviceId;
}

export function hasDeviceId() {
  return Boolean(getOrCreateDeviceId());
}

export function createDeviceHeaders(extraHeaders = {}) {
  return {
    ...extraHeaders,
    'X-Device-Id': getOrCreateDeviceId()
  };
}

export function isDeviceMissingError(error) {
  const message = [
    error?.message,
    error?.response?.data?.message,
    error?.response?.data?.msg
  ]
    .filter(Boolean)
    .join(' ');
  return message.includes('设备标识缺失')
    || (/device/i.test(message) && /missing|required|缺失/i.test(message));
}

export function safeLocalStorageGet(key) {
  try {
    return window.localStorage?.getItem(key) || '';
  } catch {
    return '';
  }
}

export function safeLocalStorageSet(key, value) {
  try {
    window.localStorage?.setItem(key, value);
    return true;
  } catch {
    return false;
  }
}

function generateDeviceId() {
  if (typeof window !== 'undefined' && window.crypto?.randomUUID) {
    return `device-${window.crypto.randomUUID()}`;
  }

  return `device-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
}

function readCookie(name) {
  if (typeof document === 'undefined') {
    return '';
  }
  const matched = document.cookie
    .split('; ')
    .find((row) => row.startsWith(`${name}=`));
  return matched ? decodeURIComponent(matched.split('=').slice(1).join('=')) : '';
}

function writeCookie(name, value) {
  if (typeof document === 'undefined') {
    return;
  }
  const maxAge = 60 * 60 * 24 * 365;
  document.cookie = `${name}=${encodeURIComponent(value)}; path=/; max-age=${maxAge}; SameSite=Lax`;
}
