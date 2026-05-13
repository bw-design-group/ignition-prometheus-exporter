package dev.bwdesigngroup.prometheus.gateway;

import java.util.Optional;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;

import com.codahale.metrics.MetricRegistry;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.script.hints.PropertiesFileDocProvider;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.rpc.GatewayRpcImplementation;
import com.inductiveautomation.ignition.gateway.web.WebResourceManager;

import dev.bwdesigngroup.prometheus.common.api.PrometheusScriptInterface;
import dev.bwdesigngroup.prometheus.common.util.PrometheusConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class PrometheusExporterGatewayHook extends AbstractGatewayModuleHook {
    private static final String SERVLET_KEY = "metrics";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private GatewayContext context;
    private WebResourceManager webResourceManager;
    private CollectorRegistry collectorRegistry;
    private PrometheusGatewayScriptModule scriptModule;

    // Shared registry for servlet access
    private static CollectorRegistry sharedRegistry;

    /**
     * Get the shared CollectorRegistry for use by the servlet
     */
    public static CollectorRegistry getSharedRegistry() {
        return sharedRegistry != null ? sharedRegistry : CollectorRegistry.defaultRegistry;
    }

    /**
     * Called to before startup. This is the chance for the module to add its
     * extension points and update persistent
     * records and schemas. None of the managers will be started up at this point,
     * but the extension point managers will
     * accept extension point types.
     */
    @Override
    public void setup(GatewayContext gatewayContext) {
        this.context = gatewayContext;
        this.webResourceManager = gatewayContext.getWebResourceManager();
        this.collectorRegistry = CollectorRegistry.defaultRegistry;

        this.scriptModule = new PrometheusGatewayScriptModule(collectorRegistry);
    }

    /**
     * Called to initialize the module. Will only be called once. Persistence
     * interface is available, but only in
     * read-only mode.
     */
    @Override
    public void startup(LicenseState licenseState) {
        // Register the existing Dropwizard exports in the default registry
        MetricRegistry metricRegistry = context.getMetricRegistry();
        collectorRegistry.register(new DropwizardExports(metricRegistry));
        sharedRegistry = collectorRegistry;
        // The servlet will serve both Dropwizard and custom metrics from the same registry
        // We need to store the registry so our custom servlet can access it
        try {
            webResourceManager.addServlet(SERVLET_KEY, PrometheusMetricsServlet.class);
        } catch (IllegalArgumentException e) {
            // This is thrown if the servlet is already registered. This can happen if the
            // module is reloaded.
            log.warn("Servlet already registered, removing and re-adding", e);
            webResourceManager.removeServlet(SERVLET_KEY);
            webResourceManager.addServlet(SERVLET_KEY, PrometheusMetricsServlet.class);
        }

        log.info("Prometheus Exporter Gateway initialized with unified metrics registry");
    }

    /**
     * Called to shutdown this module. Note that this instance will never be started
     * back up - a new one will be created
     * if a restart is desired
     */
    @Override
    public void shutdown() {
        webResourceManager.removeServlet(SERVLET_KEY);

        // Clear the shared registry
        sharedRegistry = null;
    }

    @Override
    public void initializeScriptManager(ScriptManager manager) {
        super.initializeScriptManager(manager);
        // Register Prometheus scripting functions in gateway scope
        manager.addScriptModule(
                PrometheusConstants.SCRIPT_MODULE_NAME,
                this.scriptModule,
                new PropertiesFileDocProvider());

        log.info("Prometheus gateway scripting functions registered under {}", PrometheusConstants.SCRIPT_MODULE_NAME);
    }

    /**
     * Provide RPC implementation for client/designer access to Prometheus script
     * functions. In 8.3+ the gateway hook returns an
     * {@link Optional}{@code <}{@link GatewayRpcImplementation}{@code >} built from
     * the serializer declared on the RPC interface and the handler instance.
     */
    @Override
    public Optional<GatewayRpcImplementation> getRpcImplementation() {
        return Optional.of(GatewayRpcImplementation.of(PrometheusScriptInterface.SERIALIZER, this.scriptModule));
    }

    /**
     * @return {@code true} if this is a "free" module, i.e. it does not participate
     *         in the licensing system. This is
     *         equivalent to the now defunct FreeModule attribute that could be
     *         specified in module.xml.
     */
    @Override
    public boolean isFreeModule() {
        return true;
    }

    /**
     * @return {@code true} if this module opts-in to participating in Ignition
     *         Maker Edition. Default is
     *         {@code false}. If you override this and return true, your module will
     *         become activated when running in
     *         a Maker Edition installation.
     */
    @Override
    public boolean isMakerEditionCompatible() {
        return true;
    }
}
