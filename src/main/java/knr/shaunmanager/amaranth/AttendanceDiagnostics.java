package knr.shaunmanager.amaranth;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Log only controlled metadata, never exception messages or ERP payloads. */
public final class AttendanceDiagnostics {
    private static final Logger LOG = LoggerFactory.getLogger(AttendanceDiagnostics.class);
    private static final Set<String> CODES = new HashSet<>(Arrays.asList(
        "CONFIG_KEY_MISSING", "CONFIG_KEY_INVALID", "CONFIG_DECRYPT_FAILED", "CONFIG_ENCRYPT_FAILED",
        "DISCOVERY_MISMATCH", "UPSTREAM_REQUEST_FAILED", "UPSTREAM_REQUEST_OUTCOME_UNKNOWN",
        "LOGIN_REQUIRED", "INVALID_LOCAL_CONFIG", "LOCAL_CONFIG_UNAVAILABLE", "REQUIRED_FIELD_MISSING",
        "LOGIN_REJECTED", "ATTENDANCE_REJECTED", "EMPLOYEE_NOT_FOUND", "EMPLOYEE_AMBIGUOUS",
        "EMPLOYEE_FIELD_MISSING"));
    private AttendanceDiagnostics() {}
    public static String code(Throwable ex) {
        return ex instanceof AmaranthLoginService.LoginFailure && CODES.contains(ex.getMessage())
            ? ex.getMessage() : "UNEXPECTED_FAILURE";
    }
    public static <T> T step(String stage, Supplier<T> operation) {
        long start = System.nanoTime();
        LOG.info("Attendance stage={} status=START", stage);
        try {
            T result = operation.get();
            LOG.info("Attendance stage={} status=SUCCESS elapsedMs={}", stage, (System.nanoTime()-start)/1000000);
            return result;
        } catch (RuntimeException ex) {
            LOG.warn("Attendance stage={} status=FAILED code={} exceptionType={} elapsedMs={}",
                stage, code(ex), ex.getClass().getSimpleName(), (System.nanoTime()-start)/1000000);
            throw ex;
        }
    }
}
