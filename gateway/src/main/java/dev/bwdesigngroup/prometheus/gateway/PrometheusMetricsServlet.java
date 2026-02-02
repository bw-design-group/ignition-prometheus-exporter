package dev.bwdesigngroup.prometheus.gateway;

import io.prometheus.client.exporter.MetricsServlet;

/**
 * Custom MetricsServlet that uses our unified CollectorRegistry
 * to ensure both Dropwizard and custom metrics are properly served
 * with correct type information.
 */
public class PrometheusMetricsServlet extends MetricsServlet {
    private static final long serialVersionUID = 1L;
    
    public PrometheusMetricsServlet() {
        // Use the shared registry from the gateway hook
        super(PrometheusExporterGatewayHook.getSharedRegistry());
    }
}