package com.nexusdoc.common.exception;

import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;

public final class DatabaseExceptionHelper {

    public static final String DATABASE_UNAVAILABLE_MESSAGE = "数据库未连接，请启动 MySQL 后重试";

    private DatabaseExceptionHelper() {
    }

    public static boolean isDatabaseUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CannotGetJdbcConnectionException
                    || current instanceof DataAccessException
                    || current instanceof CannotCreateTransactionException
                    || current instanceof MyBatisSystemException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
