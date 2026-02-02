package dev.bwdesigngroup.prometheus.common.scripting;

import com.inductiveautomation.ignition.client.gateway_interface.ModuleRPCFactory;
import com.inductiveautomation.ignition.common.script.hints.ScriptArg;
import com.inductiveautomation.ignition.common.script.hints.ScriptFunction;

import java.util.List;
import java.util.Map;

import dev.bwdesigngroup.prometheus.common.api.PrometheusScriptInterface;
import dev.bwdesigngroup.prometheus.common.exception.PrometheusExceptionHandler;
import dev.bwdesigngroup.prometheus.common.util.PrometheusConstants;

/**
 * RPC-based Prometheus script module implementation for Client and Designer scopes.
 * Handles all Prometheus operations via RPC calls to the Gateway.
 */
public class PrometheusRPCScriptModule extends AbstractPrometheusScriptModule {
    private final PrometheusScriptInterface rpc;

    public PrometheusRPCScriptModule() {
        this.rpc = ModuleRPCFactory.create(
                PrometheusConstants.MODULE_ID,
                PrometheusScriptInterface.class);
    }
    
    // ==================== Counter Operations ====================
    
    @Override
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void createCounter(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("description") String description, 
            @ScriptArg("labelNames") List<String> labelNames, 
            @ScriptArg(value = "errorIfExists", optional = true) boolean errorIfExists) throws Exception {
        PrometheusExceptionHandler.execute("createCounter", () -> {
            rpc.createCounter(metricName, description, labelNames, errorIfExists);
            return null;
        });
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void incrementCounter(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "value", optional = true) double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        PrometheusExceptionHandler.execute("incrementCounter", () -> {
            rpc.incrementCounter(metricName, value, labels);
            return null;
        });
    }

    // ==================== Gauge Operations ====================

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void createGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("description") String description, 
            @ScriptArg("labelNames") List<String> labelNames, 
            @ScriptArg(value = "errorIfExists", optional = true) boolean errorIfExists) throws Exception {
        PrometheusExceptionHandler.execute("createGauge", () -> {
            rpc.createGauge(metricName, description, labelNames, errorIfExists);
            return null;
        });
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void setGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("value") double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        PrometheusExceptionHandler.execute("setGauge", () -> {
            rpc.setGauge(metricName, value, labels);
            return null;
        });
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void incrementGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "value", optional = true) double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        PrometheusExceptionHandler.execute("incrementGauge", () -> {
            rpc.incrementGauge(metricName, value, labels);
            return null;
        });
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void decrementGauge(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "value", optional = true) double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        PrometheusExceptionHandler.execute("decrementGauge", () -> {
            rpc.decrementGauge(metricName, value, labels);
            return null;
        });
    }

    // ==================== Histogram Operations ====================

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void createHistogram(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("description") String description, 
            @ScriptArg("labelNames") List<String> labelNames, 
            @ScriptArg(value = "buckets", optional = true) double[] buckets, 
            @ScriptArg(value = "errorIfExists", optional = true) boolean errorIfExists) throws Exception {
        PrometheusExceptionHandler.execute("createHistogram", () -> {
            rpc.createHistogram(metricName, description, labelNames, buckets, errorIfExists);
            return null;
        });
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void observeHistogram(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg("value") double value, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        PrometheusExceptionHandler.execute("observeHistogram", () -> {
            rpc.observeHistogram(metricName, value, labels);
            return null;
        });
    }

    // ==================== Utility Operations ====================

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public double getMetricValue(
            @ScriptArg("metricName") String metricName, 
            @ScriptArg(value = "labels", optional = true) Map<String, Object> labels) throws Exception {
        return PrometheusExceptionHandler.execute("getMetricValue", () -> 
            rpc.getMetricValue(metricName, labels));
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public List<String> listMetrics() throws Exception {
        return PrometheusExceptionHandler.execute("listMetrics", () -> 
            rpc.listMetrics());
    }

    @Override
    @ScriptFunction(docBundlePrefix = "AbstractPrometheusScriptModule")
    public void removeMetric(@ScriptArg("metricName") String metricName) throws Exception {
        PrometheusExceptionHandler.execute("removeMetric", () -> {
            rpc.removeMetric(metricName);
            return null;
        });
    }
}