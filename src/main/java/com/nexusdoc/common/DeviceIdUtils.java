package com.nexusdoc.common;

import com.nexusdoc.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class DeviceIdUtils {

    public static final String DEVICE_ID_HEADER = "X-Device-Id";

    private static final int MAX_DEVICE_ID_LENGTH = 128;

    private DeviceIdUtils() {
    }

    public static String getDeviceId(HttpServletRequest request) {
        String deviceId = request == null ? null : request.getHeader(DEVICE_ID_HEADER);
        if (!StringUtils.hasText(deviceId)) {
            throw new BusinessException("设备标识缺失，请刷新页面后重试");
        }
        String normalized = deviceId.trim();
        if (normalized.length() > MAX_DEVICE_ID_LENGTH) {
            throw new BusinessException("设备标识异常，请刷新页面后重试");
        }
        return normalized;
    }
}
