package dev.bwdesigngroup.prometheus.gateway;

import io.prometheus.client.*;
import dev.bwdesigngroup.prometheus.common.api.PrometheusScriptInterface;
import dev.bwdesigngroup.prometheus.common.scripting.AbstractPrometheusScriptModule;
import dev.bwdesigngroup.prometheus.common.util.PrometheusConstants;
import dev.bwdesigngroup.prometheus.common.util.PrometheusUtils;
import dev.bwdesigngroup.prometheus.common.exception.PrometheusException;
import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.script.hints.NoHint;
import com.inductiveautomation.ignition.common.script.hints.JythonElement;
import com.inductiveautomation.ignition.common.script.hints.ScriptArg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway implementation of Prometheus scripting functions.
 * This class handles the actual creation and manipulation of Prometheus metrics
 * using the Prometheus Java client library.
 */
public class PrometheusGatewayScriptModule extends AbstractPrometheusScriptModule implements PrometheusScriptInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(PrometheusGatewayScriptModule.class);
    
    // Register the bundle for script documentation
    static {
        logger.info("Registering Prometheus script module bundle");
        BundleUtil.get().addBundle(
            AbstractPrometheusScriptModule.class.getSimpleName(),
            AbstractPrometheusScriptModule.class.getClassLoader(),
            AbstractPrometheusScriptModule.class.getName().replace('.', '/')
        );
    }
    
    private final CollectorRegistry registry;
    private final Map<String, Collector> metrics = new ConcurrentHashMap<>();
    private final Map<String, String[]> metricLabelNames = new ConcurrentHashMap<>();
    
    public PrometheusGatewayScriptModule(CollectorRegistry registry) {
        // Use the shared registry so custom metrics are served alongside Dropwizard metrics
        this.registry = registry;
        logger.info("PrometheusGatewayScriptModule initialized with shared registry");
    }
    
    /**
     * Gets the CollectorRegistry used for script-created metrics
     */
    @NoHint
    public CollectorRegistry getRegistry() {
        return registry;
    }
    
    // Counter Operations
    
    @Override
    @JythonElement(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void createCounter(
            @ScriptArg("metricName") String metricName,
            @ScriptArg("description") String description,
            @ScriptArg("labelNames") List<String> labelNames,
            @ScriptArg(value = "errorIfExists", optional = true) boolean errorIfExists) throws Exception {
        PrometheusUtils.validateMetricName(metricName);
        
        if (metrics.containsKey(metricName)) {
            if (errorIfExists) {
                throw new PrometheusException(PrometheusConstants.ERROR_METRIC_ALREADY_EXISTS + metricName);
            } else {
                logger.debug("Counter metric {} already exists, skipping creation", metricName);
                return;
            }
        }
        
        try {
            Counter.Builder builder = Counter.build()
                    .name(metricName)
                    .help(description != null ? description : PrometheusConstants.DEFAULT_HELP_TEXT);

            if (labelNames != null && !labelNames.isEmpty()) {
                for (String labelName : labelNames) {
                    PrometheusUtils.validateLabelName(labelName);
                }
                builder.labelNames(labelNamesToArray(labelNames));
            }
            
            Counter counter = builder.register(registry);
            metrics.put(metricName, counter);
            if (labelNames != null && !labelNames.isEmpty()) {
                metricLabelNames.put(metricName, labelNamesToArray(labelNames));
            }
            
            logger.debug("Created counter metric: {} with labels: {}", metricName, labelNames);
        } catch (Exception e) {
            throw new PrometheusException("Failed to create counter metric: " + metricName, e);
        }
    }
    
    @Override
    @JythonElement(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void incrementCounter(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "value", optional = true) double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        PrometheusUtils.validateMetricName(metricName);
        PrometheusUtils.validateValue(value);
        PrometheusUtils.validateLabels(labels);
        
        if (value < 0) {
            throw new PrometheusException("Counter increment value must be >= 0: " + value);
        }
        
        Collector collector = metrics.get(metricName);
        if (collector == null) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_NOT_FOUND + metricName);
        }
        
        if (!(collector instanceof Counter)) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_TYPE_MISMATCH + metricName + " (expected Counter)");
        }
        
        try {
            Counter counter = (Counter) collector;
            if (labels != null && !labels.isEmpty()) {
                counter.labels(labelValuesToArray(metricName, labels)).inc(value);
            } else {
                counter.inc(value);
            }
            
            logger.trace("Incremented counter {} by {} with labels: {}", metricName, value, labels);
        } catch (Exception e) {
            throw new PrometheusException("Failed to increment counter: " + metricName, e);
        }
    }
    
    // Gauge Operations
    
    @Override
    @JythonElement(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void createGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("description") String description, 
            @ScriptArg("labelNames") List<String> labelNames, 
            @ScriptArg(value = "errorIfExists", optional = true) boolean errorIfExists) throws Exception {
        PrometheusUtils.validateMetricName(metricName);
        
        if (metrics.containsKey(metricName)) {
            if (errorIfExists) {
                throw new PrometheusException(PrometheusConstants.ERROR_METRIC_ALREADY_EXISTS + metricName);
            } else {
                logger.debug("Gauge metric {} already exists, skipping creation", metricName);
                return;
            }
        }
        
        try {
            Gauge.Builder builder = Gauge.build()
                    .name(metricName)
                    .help(description != null ? description : PrometheusConstants.DEFAULT_HELP_TEXT);
            
            if (labelNames != null && !labelNames.isEmpty()) {
                for (String labelName : labelNames) {
                    PrometheusUtils.validateLabelName(labelName);
                }
                builder.labelNames(labelNamesToArray(labelNames));
            }
            
            Gauge gauge = builder.register(registry);
            metrics.put(metricName, gauge);
            if (labelNames != null && !labelNames.isEmpty()) {
                metricLabelNames.put(metricName, labelNamesToArray(labelNames));
            }
            
            logger.debug("Created gauge metric: {} with labels: {}", metricName, labelNames);
        } catch (Exception e) {
            throw new PrometheusException("Failed to create gauge metric: " + metricName, e);
        }
    }
    
    @Override
    @JythonElement(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void setGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("value") double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        PrometheusUtils.validateMetricName(metricName);
        PrometheusUtils.validateValue(value);
        PrometheusUtils.validateLabels(labels);
        
        Collector collector = metrics.get(metricName);
        if (collector == null) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_NOT_FOUND + metricName);
        }
        
        if (!(collector instanceof Gauge)) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_TYPE_MISMATCH + metricName + " (expected Gauge)");
        }
        
        try {
            Gauge gauge = (Gauge) collector;
            if (labels != null && !labels.isEmpty()) {
                gauge.labels(labelValuesToArray(metricName, labels)).set(value);
            } else {
                gauge.set(value);
            }
            
            logger.trace("Set gauge {} to {} with labels: {}", metricName, value, labels);
        } catch (Exception e) {
            throw new PrometheusException("Failed to set gauge: " + metricName, e);
        }
    }
    
    @Override
    @JythonElement(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void incrementGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "value", optional = true) double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        PrometheusUtils.validateMetricName(metricName);
        PrometheusUtils.validateValue(value);
        PrometheusUtils.validateLabels(labels);
        
        Collector collector = metrics.get(metricName);
        if (collector == null) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_NOT_FOUND + metricName);
        }
        
        if (!(collector instanceof Gauge)) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_TYPE_MISMATCH + metricName + " (expected Gauge)");
        }
        
        try {
            Gauge gauge = (Gauge) collector;
            if (labels != null && !labels.isEmpty()) {
                gauge.labels(labelValuesToArray(metricName, labels)).inc(value);
            } else {
                gauge.inc(value);
            }
            
            logger.trace("Incremented gauge {} by {} with labels: {}", metricName, value, labels);
        } catch (Exception e) {
            throw new PrometheusException("Failed to increment gauge: " + metricName, e);
        }
    }
    
    @Override
    @JythonElement(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void decrementGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "value", optional = true) double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        PrometheusUtils.validateMetricName(metricName);
        PrometheusUtils.validateValue(value);
        PrometheusUtils.validateLabels(labels);
        
        Collector collector = metrics.get(metricName);
        if (collector == null) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_NOT_FOUND + metricName);
        }
        
        if (!(collector instanceof Gauge)) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_TYPE_MISMATCH + metricName + " (expected Gauge)");
        }
        
        try {
            Gauge gauge = (Gauge) collector;
            if (labels != null && !labels.isEmpty()) {
                gauge.labels(labelValuesToArray(metricName, labels)).dec(value);
            } else {
                gauge.dec(value);
            }
            
            logger.trace("Decremented gauge {} by {} with labels: {}", metricName, value, labels);
        } catch (Exception e) {
            throw new PrometheusException("Failed to decrement gauge: " + metricName, e);
        }
    }
    
    // Histogram Operations
    
    @Override
    @JythonElement(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void createHistogram(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("description") String description, 
            @ScriptArg("labelNames") List<String> labelNames, 
            @ScriptArg(value = "buckets", optional = true) double[] buckets, 
            @ScriptArg(value = "errorIfExists", optional = true) boolean errorIfExists) throws Exception {
        PrometheusUtils.validateMetricName(metricName);
        
        // Use default buckets if none provided
        if (buckets == null || buckets.length == 0) {
            buckets = PrometheusConstants.DEFAULT_HISTOGRAM_BUCKETS;
        } else {
            PrometheusUtils.validateHistogramBuckets(buckets);
        }
        
        if (metrics.containsKey(metricName)) {
            if (errorIfExists) {
                throw new PrometheusException(PrometheusConstants.ERROR_METRIC_ALREADY_EXISTS + metricName);
            } else {
                logger.debug("Histogram metric {} already exists, skipping creation", metricName);
                return;
            }
        }
        
        try {
            Histogram.Builder builder = Histogram.build()
                    .name(metricName)
                    .help(description != null ? description : PrometheusConstants.DEFAULT_HELP_TEXT)
                    .buckets(buckets);
            
            if (labelNames != null && !labelNames.isEmpty()) {
                for (String labelName : labelNames) {
                    PrometheusUtils.validateLabelName(labelName);
                }
                builder.labelNames(labelNamesToArray(labelNames));
            }
            
            Histogram histogram = builder.register(registry);
            metrics.put(metricName, histogram);
            if (labelNames != null && !labelNames.isEmpty()) {
                metricLabelNames.put(metricName, labelNamesToArray(labelNames));
            }
            
            logger.debug("Created histogram metric: {} with labels: {} and {} buckets", metricName, labelNames, buckets.length);
        } catch (Exception e) {
            throw new PrometheusException("Failed to create histogram metric: " + metricName, e);
        }
    }
    
    @Override
    @JythonElement(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void observeHistogram(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("value") double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        PrometheusUtils.validateMetricName(metricName);
        PrometheusUtils.validateValue(value);
        PrometheusUtils.validateLabels(labels);
        
        Collector collector = metrics.get(metricName);
        if (collector == null) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_NOT_FOUND + metricName);
        }
        
        if (!(collector instanceof Histogram)) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_TYPE_MISMATCH + metricName + " (expected Histogram)");
        }
        
        try {
            Histogram histogram = (Histogram) collector;
            if (labels != null && !labels.isEmpty()) {
                histogram.labels(labelValuesToArray(metricName, labels)).observe(value);
            } else {
                histogram.observe(value);
            }
            
            logger.trace("Observed value {} in histogram {} with labels: {}", value, metricName, labels);
        } catch (Exception e) {
            throw new PrometheusException("Failed to observe histogram: " + metricName, e);
        }
    }
    
    // Utility Operations
    
    @Override
    @JythonElement(docBundlePrefix = "AbstractPrometheusScriptModule")
    public List<String> listMetrics() throws Exception {
        try {
            List<String> metricNames = new ArrayList<>(metrics.keySet());
            Collections.sort(metricNames);
            logger.debug("Listed {} custom metrics", metricNames.size());
            return metricNames;
        } catch (Exception e) {
            throw new PrometheusException("Failed to list metrics", e);
        }
    }
    
    @Override
    @JythonElement(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void removeMetric(
            @ScriptArg("metricName") String metricName) throws Exception {
        PrometheusUtils.validateMetricName(metricName);
        
        Collector collector = metrics.remove(metricName);
        if (collector == null) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_NOT_FOUND + metricName);
        }
        
        try {
            registry.unregister(collector);
            metricLabelNames.remove(metricName);
            logger.debug("Removed metric: {}", metricName);
        } catch (Exception e) {
            // Re-add to map if unregister failed
            metrics.put(metricName, collector);
            throw new PrometheusException("Failed to remove metric: " + metricName, e);
        }
    }
    
    @Override
    @JythonElement(docBundlePrefix = "AbstractPrometheusScriptModule")
    public double getMetricValue(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        PrometheusUtils.validateMetricName(metricName);
        PrometheusUtils.validateLabels(labels);
        
        Collector collector = metrics.get(metricName);
        if (collector == null) {
            throw new PrometheusException(PrometheusConstants.ERROR_METRIC_NOT_FOUND + metricName);
        }
        
        try {
            // This is a simplified implementation - getting exact values from Prometheus metrics
            // can be complex due to the multi-sample nature of some metrics
            if (collector instanceof Gauge) {
                Gauge gauge = (Gauge) collector;
                if (labels != null && !labels.isEmpty()) {
                    return gauge.labels(labelValuesToArray(metricName, labels)).get();
                } else {
                    return gauge.get();
                }
            } else if (collector instanceof Counter) {
                Counter counter = (Counter) collector;
                if (labels != null && !labels.isEmpty()) {
                    return counter.labels(labelValuesToArray(metricName, labels)).get();
                } else {
                    return counter.get();
                }
            } else {
                // For histograms, we could return the count, sum, or a specific percentile
                // For simplicity, we'll throw an exception for now
                throw new PrometheusException("Getting values from histogram metrics is not supported yet");
            }
        } catch (Exception e) {
            throw new PrometheusException("Failed to get metric value: " + metricName, e);
        }
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Convert label names list to string array for Prometheus builders
     */
    private static String[] labelNamesToArray(List<String> labelNames) {
        return labelNames != null && !labelNames.isEmpty() 
            ? labelNames.toArray(new String[0]) 
            : new String[0];
    }
    
    /**
     * Convert labels map values to string array for Prometheus metric labeling.
     * Automatically converts non-string values to strings using toString().
     * Values are returned in the same order as the label names were defined when the metric was created.
     */
    private String[] labelValuesToArray(String metricName, Map<String, Object> labels) {
        if (labels == null || labels.isEmpty()) {
            return new String[0];
        }
        
        String[] labelNames = metricLabelNames.get(metricName);
        if (labelNames == null || labelNames.length == 0) {
            // Fallback to original behavior if no label names stored (shouldn't happen for properly created metrics)
            return labels.values().stream()
                .map(value -> value != null ? value.toString() : "null")
                .toArray(String[]::new);
        }
        
        // Return values in the same order as the label names were defined
        String[] values = new String[labelNames.length];
        for (int i = 0; i < labelNames.length; i++) {
            Object value = labels.get(labelNames[i]);
            values[i] = value != null ? value.toString() : "null";
        }
        
        return values;
    }
}