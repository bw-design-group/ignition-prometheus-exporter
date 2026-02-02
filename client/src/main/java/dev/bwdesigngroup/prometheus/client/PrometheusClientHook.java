package dev.bwdesigngroup.prometheus.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.bwdesigngroup.prometheus.common.util.PrometheusConstants;
import dev.bwdesigngroup.prometheus.common.scripting.PrometheusRPCScriptModule;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.script.hints.PropertiesFileDocProvider;
import com.inductiveautomation.vision.api.client.AbstractClientModuleHook;

/**
 * Client module hook for Prometheus Exporter
 * Registers scripting functions for Vision client scope
 */
public class PrometheusClientHook extends AbstractClientModuleHook {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusClientHook.class);

    @Override
    public void initializeScriptManager(ScriptManager manager) {
        super.initializeScriptManager(manager);

        try {
            // Register Prometheus scripting module in client scope
            manager.addScriptModule(
                    PrometheusConstants.SCRIPT_MODULE_NAME,
                    new PrometheusRPCScriptModule(),
                    new PropertiesFileDocProvider());

            logger.info("Prometheus client scripting functions registered under {}", PrometheusConstants.SCRIPT_MODULE_NAME);
        } catch (Exception e) {
            logger.error("Failed to register Prometheus client scripting functions", e);
        }
    }
}