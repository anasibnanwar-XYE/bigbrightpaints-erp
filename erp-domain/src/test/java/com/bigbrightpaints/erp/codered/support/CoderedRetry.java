package com.bigbrightpaints.erp.codered.support;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import org.postgresql.util.PSQLException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;

import java.util.Set;

public final class CoderedRetry {

    private static final Set<String> POSTGRES_TRANSIENT_SQLSTATES = Set.of(
            "40001", // serialization_failure
            "40P01", // deadlock_detected
            "55P03", // lock_not_available
            "57014"  // query_canceled (e.g., statement timeout)
    );

    private CoderedRetry() {
    }

    public static boolean isRetryable(Throwable error) {
        if (error == null) {
            return false;
        }
        if (error instanceof ApplicationException) {
            return false;
        }
        if (error instanceof CannotAcquireLockException
                || error instanceof PessimisticLockingFailureException
                || error instanceof OptimisticLockingFailureException
                || error instanceof TransientDataAccessException) {
            return true;
        }
        if (error instanceof DataAccessException dae) {
            Throwable root = rootCause(dae);
            if (root instanceof PSQLException psql) {
                return POSTGRES_TRANSIENT_SQLSTATES.contains(psql.getSQLState());
            }
        }
        Throwable root = rootCause(error);
        if (root instanceof PSQLException psql) {
            return POSTGRES_TRANSIENT_SQLSTATES.contains(psql.getSQLState());
        }
        return false;
    }

    private static Throwable rootCause(Throwable error) {
        Throwable cursor = error;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor;
    }
}

