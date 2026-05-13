package dev.bwdesigngroup.prometheus.gateway;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Jakarta-based metrics servlet for Ignition 8.3+.
 *
 * Ignition 8.3 webserver uses {@code jakarta.servlet} APIs, whereas the
 * bundled Prometheus 0.16 {@code MetricsServlet} extends
 * {@code javax.servlet.http.HttpServlet}. This servlet replaces that bundled
 * servlet by writing the Prometheus exposition format directly using
 * {@link TextFormat}.
 */
public class PrometheusMetricsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final CollectorRegistry registry;

    public PrometheusMetricsServlet() {
        // Use the shared registry from the gateway hook
        this.registry = PrometheusExporterGatewayHook.getSharedRegistry();
    }

    public PrometheusMetricsServlet(CollectorRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String contentType = TextFormat.chooseContentType(req.getHeader("Accept"));
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(contentType);

        Set<String> includedNameSet = parseNames(req.getParameterValues("name[]"));

        try (ServletOutputStream out = resp.getOutputStream();
             Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            TextFormat.writeFormat(
                    contentType,
                    writer,
                    registry.filteredMetricFamilySamples(includedNameSet));
            writer.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    private static Set<String> parseNames(String[] params) {
        Set<String> names = new HashSet<>();
        if (params != null) {
            for (String name : params) {
                if (name != null) {
                    names.add(name);
                }
            }
        }
        return names;
    }

}
