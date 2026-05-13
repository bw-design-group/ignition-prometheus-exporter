package dev.bwdesigngroup.prometheus.common.api;

import java.util.List;
import java.util.Map;

import com.inductiveautomation.ignition.common.rpc.RpcInterface;
import com.inductiveautomation.ignition.common.rpc.RpcSerializer;
import com.inductiveautomation.ignition.common.rpc.proto.ProtoRpcSerializer;

/**
 * RPC interface for Prometheus metrics scripting functions.
 * This interface defines all the methods that can be called from Client/Designer scopes
 * and will be executed on the Gateway scope via RPC.
 */
@RpcInterface(packageId = "dev.bwdesigngroup.prometheus")
public interface PrometheusScriptInterface {

    RpcSerializer SERIALIZER = ProtoRpcSerializer.newBuilder().build();

    // Counter Operations

    /**
     * Creates a new counter metric.
     *
     * @param metricName The name of the metric
     * @param description Help text describing the metric
     * @param labelNames List of label names for this metric
     * @param errorIfExists Whether to throw an error if the metric already exists (default: false)
     * @throws Exception if metric creation fails
     */
    void createCounter(String metricName, String description, List<String> labelNames, boolean errorIfExists) throws Exception;

    /**
     * Increments a counter metric.
     *
     * @param metricName The name of the metric
     * @param value The amount to increment by (default 1.0)
     * @param labels Map of label names to values (can be null)
     * @throws Exception if increment fails
     */
    void incrementCounter(String metricName, double value, Map<String, Object> labels) throws Exception;

    // Gauge Operations

    /**
     * Creates a new gauge metric.
     *
     * @param metricName The name of the metric
     * @param description Help text describing the metric
     * @param labelNames List of label names for this metric
     * @param errorIfExists Whether to throw an error if the metric already exists (default: false)
     * @throws Exception if metric creation fails
     */
    void createGauge(String metricName, String description, List<String> labelNames, boolean errorIfExists) throws Exception;

    /**
     * Sets a gauge metric to a specific value.
     *
     * @param metricName The name of the metric
     * @param value The value to set
     * @param labels Map of label names to values (can be null)
     * @throws Exception if set operation fails
     */
    void setGauge(String metricName, double value, Map<String, Object> labels) throws Exception;

    /**
     * Increments a gauge metric.
     *
     * @param metricName The name of the metric
     * @param value The amount to increment by (default 1.0)
     * @param labels Map of label names to values (can be null)
     * @throws Exception if increment fails
     */
    void incrementGauge(String metricName, double value, Map<String, Object> labels) throws Exception;

    /**
     * Decrements a gauge metric.
     *
     * @param metricName The name of the metric
     * @param value The amount to decrement by (default 1.0)
     * @param labels Map of label names to values (can be null)
     * @throws Exception if decrement fails
     */
    void decrementGauge(String metricName, double value, Map<String, Object> labels) throws Exception;

    // Histogram Operations

    /**
     * Creates a new histogram metric.
     *
     * @param metricName The name of the metric
     * @param description Help text describing the metric
     * @param labelNames List of label names for this metric
     * @param buckets Array of bucket boundaries (can be null for defaults)
     * @param errorIfExists Whether to throw an error if the metric already exists (default: false)
     * @throws Exception if metric creation fails
     */
    void createHistogram(String metricName, String description, List<String> labelNames, double[] buckets, boolean errorIfExists) throws Exception;

    /**
     * Observes a value in a histogram metric.
     *
     * @param metricName The name of the metric
     * @param value The value to observe
     * @param labels Map of label names to values (can be null)
     * @throws Exception if observation fails
     */
    void observeHistogram(String metricName, double value, Map<String, Object> labels) throws Exception;

    // Utility Operations

    /**
     * Lists all registered custom metrics.
     *
     * @return List of metric names
     * @throws Exception if listing fails
     */
    List<String> listMetrics() throws Exception;

    /**
     * Removes a metric from the registry.
     *
     * @param metricName The name of the metric to remove
     * @throws Exception if removal fails
     */
    void removeMetric(String metricName) throws Exception;

    /**
     * Gets the current value of a metric.
     *
     * @param metricName The name of the metric
     * @param labels Map of label names to values (can be null)
     * @return The current value of the metric
     * @throws Exception if retrieval fails
     */
    double getMetricValue(String metricName, Map<String, Object> labels) throws Exception;
}
