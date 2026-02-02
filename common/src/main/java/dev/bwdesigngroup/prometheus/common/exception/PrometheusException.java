package dev.bwdesigngroup.prometheus.common.exception;

import org.python.core.Py;
import org.python.core.PyException;

/**
 * Custom exception class for Prometheus operations with Jython compatibility
 */
public class PrometheusException extends Exception {
    private static final long serialVersionUID = 1L;

    public PrometheusException(String message) {
        super(message);
    }

    public PrometheusException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrometheusException(Throwable cause) {
        super(cause);
    }

    public PrometheusException(String operation, String message) {
        super(formatError(operation, message));
    }

    public PrometheusException(String operation, String message, Throwable cause) {
        super(formatError(operation, message), cause);
    }

    /**
     * Convert this exception to a Python-compatible PyException
     * Creates a custom PrometheusError type in Python for better error handling
     */
    public PyException toPythonException() {
        String message = getMessage();

        // Determine the appropriate Python exception type based on the error
        if (message.toLowerCase().contains("not found")) {
            return Py.KeyError(message);
        } else if (message.toLowerCase().contains("invalid") || message.toLowerCase().contains("validation")) {
            return Py.ValueError(message);
        } else if (message.toLowerCase().contains("timeout") || message.toLowerCase().contains("network")) {
            return Py.IOError(message);
        } else {
            // Create a custom PrometheusError instead of generic RuntimeError
            return createPrometheusError(message);
        }
    }
    
    /**
     * Create a custom Python exception type for Prometheus-specific errors
     */
    private PyException createPrometheusError(String message) {
        // For now, use RuntimeError but with a clear prefix to identify it as a Prometheus error
        return Py.RuntimeError("PrometheusError: " + message);
    }

    private static String formatError(String operation, String message) {
        return String.format("Prometheus %s operation failed: %s", operation, message);
    }
}