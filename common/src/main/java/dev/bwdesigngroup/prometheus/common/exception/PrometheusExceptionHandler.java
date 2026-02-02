package dev.bwdesigngroup.prometheus.common.exception;

import org.python.core.PyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified exception handler for Prometheus operations with Jython compatibility
 */
public class PrometheusExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusExceptionHandler.class);

    private PrometheusExceptionHandler() {
        // Utility class
    }

    /**
     * Execute any operation with proper Jython-compatible exception handling
     * Works for all contexts: scripting, RPC, and result wrappers
     */
    public static <T> T execute(String operation, Callable<T> callable) throws PyException {
        try {
            return callable.call();
        } catch (PyException e) {
            // Re-throw PyExceptions as-is - they're already Python-compatible
            logger.debug("PyException in Prometheus operation '{}': {}", operation, e.getMessage());
            throw e;
        } catch (PrometheusException e) {
            // Convert our custom exceptions to PyException
            logger.error("Prometheus operation '{}' failed: {}", operation, e.getMessage(), e);
            throw e.toPythonException();
        } catch (Exception e) {
            // Wrap all other exceptions in PrometheusException first, then convert to Python
            String errorMsg = extractErrorMessage(e);
            String formattedError = formatError(operation, errorMsg);
            logger.error("Prometheus operation '{}' failed: {}", operation, errorMsg, e);
            
            // Create a PrometheusException and convert it to Python exception
            PrometheusException prometheusException = new PrometheusException(formattedError, e);
            throw prometheusException.toPythonException();
        }
    }

    /**
     * Extract meaningful error message from exception chain
     */
    private static String extractErrorMessage(Exception e) {
        String errorMsg = e.getMessage();
        if (errorMsg != null && !errorMsg.isEmpty()) {
            // Clean up common RPC error prefixes
            errorMsg = errorMsg.replaceFirst("^java\\.lang\\.RuntimeException: ", "")
                    .replaceFirst(
                            "^com\\.inductiveautomation\\.ignition\\.client\\.gateway_interface\\.GatewayException: ",
                            "");
            return errorMsg;
        }

        // Walk the cause chain
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                return cause.getMessage();
            }
            cause = cause.getCause();
        }

        return "Unknown error: " + e.getClass().getSimpleName();
    }

    /**
     * Format error message with operation context
     */
    private static String formatError(String operation, String message) {
        return String.format("Prometheus %s operation failed: %s", operation, message);
    }

    /**
     * Single functional interface for all operations
     */
    @FunctionalInterface
    public interface Callable<T> {
        T call() throws Exception;
    }
}