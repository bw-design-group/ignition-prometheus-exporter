package dev.bwdesigngroup.prometheus.common.scripting;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.script.hints.NoHint;
import com.inductiveautomation.ignition.common.script.hints.ScriptFunction;
import com.inductiveautomation.ignition.common.script.hints.ScriptArg;
import dev.bwdesigngroup.prometheus.common.util.PrometheusConstants;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for Prometheus scripting functions.
 * Defines the core abstract methods and convenience overloads.
 */
public abstract class AbstractPrometheusScriptModule {
    private static final Logger logger = LoggerFactory.getLogger(AbstractPrometheusScriptModule.class);

    // Register the bundle for script documentation
    static {
        logger.info("Registering Prometheus script module bundle");
        BundleUtil.get().addBundle(
            AbstractPrometheusScriptModule.class.getSimpleName(),
            AbstractPrometheusScriptModule.class.getClassLoader(),
            AbstractPrometheusScriptModule.class.getName().replace('.', '/')
        );
    }
    
    // ==================== Counter Operations ====================
    
    // Main signature: createCounter(metricName, description, labelNames, errorIfExists=False)
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public abstract void createCounter(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("description") String description, 
            @ScriptArg("labelNames") List<String> labelNames, 
            @ScriptArg(value = "errorIfExists", optional = true) boolean errorIfExists) throws Exception;
    
    @NoHint
    public void createCounter(String metricName, String description, List<String> labelNames) throws Exception {
        createCounter(metricName, description, labelNames, false);
    }
    
    // Main signature: incrementCounter(metricName, value=1.0, labels={})
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public abstract void incrementCounter(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "value", optional = true) double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception;
    
    @NoHint
    public void incrementCounter(String metricName) throws Exception {
        incrementCounter(metricName, PrometheusConstants.DEFAULT_INCREMENT, null);
    }
    
    @NoHint
    public void incrementCounter(String metricName, double value) throws Exception {
        incrementCounter(metricName, value, null);
    }
    
    @NoHint
    public void incrementCounter(String metricName, Map<String, Object> labels) throws Exception {
        incrementCounter(metricName, PrometheusConstants.DEFAULT_INCREMENT, labels);
    }
    
    // ==================== Gauge Operations ====================
    
    // Main signature: createGauge(metricName, description, labelNames, errorIfExists=False)
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public abstract void createGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("description") String description, 
            @ScriptArg("labelNames") List<String> labelNames, 
            @ScriptArg(value = "errorIfExists", optional = true) boolean errorIfExists) throws Exception;
    
    @NoHint
    public void createGauge(String metricName, String description, List<String> labelNames) throws Exception {
        createGauge(metricName, description, labelNames, false);
    }
    
    // Main signature: setGauge(metricName, value, labels={})
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public abstract void setGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("value") double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception;
    
    @NoHint
    public void setGauge(String metricName, double value) throws Exception {
        setGauge(metricName, value, null);
    }
    
    // Main signature: incrementGauge(metricName, value=1.0, labels={})
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public abstract void incrementGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "value", optional = true) double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception;
    
    @NoHint
    public void incrementGauge(String metricName) throws Exception {
        incrementGauge(metricName, PrometheusConstants.DEFAULT_INCREMENT, null);
    }
    
    @NoHint
    public void incrementGauge(String metricName, double value) throws Exception {
        incrementGauge(metricName, value, null);
    }
    
    @NoHint
    public void incrementGauge(String metricName, Map<String, Object> labels) throws Exception {
        incrementGauge(metricName, PrometheusConstants.DEFAULT_INCREMENT, labels);
    }
    
    // Main signature: decrementGauge(metricName, value=1.0, labels={})
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public abstract void decrementGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "value", optional = true) double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception;
    
    @NoHint
    public void decrementGauge(String metricName) throws Exception {
        decrementGauge(metricName, PrometheusConstants.DEFAULT_INCREMENT, null);
    }
    
    @NoHint
    public void decrementGauge(String metricName, double value) throws Exception {
        decrementGauge(metricName, value, null);
    }
    
    @NoHint
    public void decrementGauge(String metricName, Map<String, Object> labels) throws Exception {
        decrementGauge(metricName, PrometheusConstants.DEFAULT_INCREMENT, labels);
    }
    
    // ==================== Histogram Operations ====================
    
    // Main signature: createHistogram(metricName, description, labelNames, buckets=DEFAULT, errorIfExists=False)
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public abstract void createHistogram(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("description") String description, 
            @ScriptArg("labelNames") List<String> labelNames, 
            @ScriptArg(value = "buckets", optional = true) double[] buckets, 
            @ScriptArg(value = "errorIfExists", optional = true) boolean errorIfExists) throws Exception;
    
    @NoHint
    public void createHistogram(String metricName, String description, List<String> labelNames) throws Exception {
        createHistogram(metricName, description, labelNames, PrometheusConstants.DEFAULT_HISTOGRAM_BUCKETS, false);
    }
    
    @NoHint
    public void createHistogram(String metricName, String description, List<String> labelNames, double[] buckets) throws Exception {
        createHistogram(metricName, description, labelNames, buckets, false);
    }
    
    // Main signature: observeHistogram(metricName, value, labels={})
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public abstract void observeHistogram(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("value") double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception;
    
    @NoHint
    public void observeHistogram(String metricName, double value) throws Exception {
        observeHistogram(metricName, value, null);
    }
    
    // ==================== Utility Operations ====================
    
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public abstract List<String> listMetrics() throws Exception;
    
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public abstract void removeMetric(
            @ScriptArg("metricName") String metricName) throws Exception;
    
    // Main signature: getMetricValue(metricName, labels={})
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public abstract double getMetricValue(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception;
    
    @NoHint
    public double getMetricValue(String metricName) throws Exception {
        return getMetricValue(metricName, null);
    }
}