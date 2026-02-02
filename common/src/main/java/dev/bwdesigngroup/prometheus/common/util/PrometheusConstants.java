package dev.bwdesigngroup.prometheus.common.util;

/**
 * Constants used throughout the Prometheus Exporter module.
 */
public class PrometheusConstants {
    
    // Module Information
    public static final String MODULE_ID = "dev.bwdesigngroup.prometheus.PrometheusExporter";
    public static final String MODULE_NAME = "Prometheus Metrics Exporter";
    
    // Script Module Names
    public static final String SCRIPT_MODULE_NAME = "system.prometheus";
    public static final String SCRIPT_MODULE_DOC = "Functions for creating and managing Prometheus metrics";
    
    // Default Values
    public static final String DEFAULT_HELP_TEXT = "";
    public static final double DEFAULT_INCREMENT = 1.0;
    
    // Metric Name Validation
    public static final String METRIC_NAME_PATTERN = "^[a-zA-Z_:][a-zA-Z0-9_:]*$";
    public static final String LABEL_NAME_PATTERN = "^[a-zA-Z_][a-zA-Z0-9_]*$";
    
    // Default Histogram Buckets (Prometheus standard)
    public static final double[] DEFAULT_HISTOGRAM_BUCKETS = {
        0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0
    };
    
    // Error Messages
    public static final String ERROR_INVALID_METRIC_NAME = "Invalid metric name. Must match pattern: " + METRIC_NAME_PATTERN;
    public static final String ERROR_INVALID_LABEL_NAME = "Invalid label name. Must match pattern: " + LABEL_NAME_PATTERN;
    public static final String ERROR_METRIC_NOT_FOUND = "Metric not found: ";
    public static final String ERROR_METRIC_ALREADY_EXISTS = "Metric already exists: ";
    public static final String ERROR_METRIC_TYPE_MISMATCH = "Metric type mismatch for: ";
    public static final String ERROR_INVALID_VALUE = "Invalid metric value";
    public static final String ERROR_GATEWAY_COMMUNICATION = "Failed to communicate with gateway";
    
    private PrometheusConstants() {
        // Utility class - prevent instantiation
    }
}