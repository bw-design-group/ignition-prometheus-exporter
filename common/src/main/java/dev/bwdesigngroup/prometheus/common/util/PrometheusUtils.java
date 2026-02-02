package dev.bwdesigngroup.prometheus.common.util;

import dev.bwdesigngroup.prometheus.common.exception.PrometheusException;

import java.util.regex.Pattern;
import java.util.Map;

/**
 * Utility functions for Prometheus metric operations.
 */
public class PrometheusUtils {
    
    private static final Pattern METRIC_NAME_REGEX = Pattern.compile(PrometheusConstants.METRIC_NAME_PATTERN);
    private static final Pattern LABEL_NAME_REGEX = Pattern.compile(PrometheusConstants.LABEL_NAME_PATTERN);
    
    private PrometheusUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Validates a Prometheus metric name.
     * 
     * @param metricName The metric name to validate
     * @throws PrometheusException if the name is invalid
     */
    public static void validateMetricName(String metricName) throws PrometheusException {
        if (metricName == null || metricName.isEmpty()) {
            throw new PrometheusException("Metric name cannot be null or empty");
        }
        
        if (!METRIC_NAME_REGEX.matcher(metricName).matches()) {
            throw new PrometheusException(PrometheusConstants.ERROR_INVALID_METRIC_NAME);
        }
    }
    
    /**
     * Validates a Prometheus label name.
     * 
     * @param labelName The label name to validate
     * @throws PrometheusException if the name is invalid
     */
    public static void validateLabelName(String labelName) throws PrometheusException {
        if (labelName == null || labelName.isEmpty()) {
            throw new PrometheusException("Label name cannot be null or empty");
        }
        
        if (!LABEL_NAME_REGEX.matcher(labelName).matches()) {
            throw new PrometheusException(PrometheusConstants.ERROR_INVALID_LABEL_NAME + ": " + labelName);
        }
        
        // Reserved label names (starting with __ are reserved for internal use)
        if (labelName.startsWith("__")) {
            throw new PrometheusException("Label names starting with '__' are reserved: " + labelName);
        }
    }
    
    /**
     * Validates all label names in a map.
     * 
     * @param labels The labels to validate
     * @throws PrometheusException if any label name is invalid
     */
    public static void validateLabels(Map<String, Object> labels) throws PrometheusException {
        if (labels == null) {
            return; // null labels are allowed
        }
        
        for (String labelName : labels.keySet()) {
            validateLabelName(labelName);
        }
    }
    
    /**
     * Validates a metric value.
     * 
     * @param value The value to validate
     * @throws PrometheusException if the value is invalid
     */
    public static void validateValue(double value) throws PrometheusException {
        if (Double.isNaN(value)) {
            throw new PrometheusException(PrometheusConstants.ERROR_INVALID_VALUE + ": NaN");
        }
        
        if (Double.isInfinite(value)) {
            throw new PrometheusException(PrometheusConstants.ERROR_INVALID_VALUE + ": Infinite");
        }
    }
    
    /**
     * Validates histogram buckets.
     * 
     * @param buckets The bucket boundaries to validate
     * @throws PrometheusException if the buckets are invalid
     */
    public static void validateHistogramBuckets(double[] buckets) throws PrometheusException {
        if (buckets == null || buckets.length == 0) {
            throw new PrometheusException("Histogram buckets cannot be null or empty");
        }
        
        for (int i = 0; i < buckets.length; i++) {
            double bucket = buckets[i];
            
            // Validate each bucket value
            if (Double.isNaN(bucket) || Double.isInfinite(bucket)) {
                throw new PrometheusException("Invalid bucket value at index " + i + ": " + bucket);
            }
            
            // Check that buckets are in ascending order
            if (i > 0 && bucket <= buckets[i - 1]) {
                throw new PrometheusException("Histogram buckets must be in ascending order. " +
                    "Bucket at index " + i + " (" + bucket + ") is not greater than previous bucket (" + buckets[i - 1] + ")");
            }
        }
    }
    
    /**
     * Sanitizes a metric name by replacing invalid characters.
     * 
     * @param metricName The original metric name
     * @return A sanitized metric name
     */
    public static String sanitizeMetricName(String metricName) {
        if (metricName == null || metricName.isEmpty()) {
            return "unnamed_metric";
        }
        
        // Replace invalid characters with underscores
        String sanitized = metricName.replaceAll("[^a-zA-Z0-9_:]", "_");
        
        // Ensure it starts with a letter, underscore, or colon
        if (!sanitized.matches("^[a-zA-Z_:].*")) {
            sanitized = "_" + sanitized;
        }
        
        return sanitized;
    }
    
    /**
     * Sanitizes a label name by replacing invalid characters.
     * 
     * @param labelName The original label name
     * @return A sanitized label name
     */
    public static String sanitizeLabelName(String labelName) {
        if (labelName == null || labelName.isEmpty()) {
            return "unnamed_label";
        }
        
        // Replace invalid characters with underscores
        String sanitized = labelName.replaceAll("[^a-zA-Z0-9_]", "_");
        
        // Ensure it starts with a letter or underscore
        if (!sanitized.matches("^[a-zA-Z_].*")) {
            sanitized = "_" + sanitized;
        }
        
        // Ensure it doesn't start with __ (reserved)
        if (sanitized.startsWith("__")) {
            sanitized = "custom_" + sanitized.substring(2);
        }
        
        return sanitized;
    }
}