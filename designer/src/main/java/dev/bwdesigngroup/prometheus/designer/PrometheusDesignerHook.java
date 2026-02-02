package dev.bwdesigngroup.prometheus.designer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.bwdesigngroup.prometheus.common.util.PrometheusConstants;
import dev.bwdesigngroup.prometheus.common.scripting.PrometheusRPCScriptModule;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.script.hints.PropertiesFileDocProvider;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;

/**
 * Designer module hook for Prometheus Exporter
 * Registers scripting functions for Designer scope
 */
public class PrometheusDesignerHook extends AbstractDesignerModuleHook {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusDesignerHook.class);

    @Override
    public void initializeScriptManager(ScriptManager manager) {
        super.initializeScriptManager(manager);

        try {
            // Register Prometheus scripting module in designer scope
            manager.addScriptModule(
                    PrometheusConstants.SCRIPT_MODULE_NAME,
                    new PrometheusRPCScriptModule(),
                    new PropertiesFileDocProvider());

            logger.info("Prometheus designer scripting functions registered under {}", PrometheusConstants.SCRIPT_MODULE_NAME);
        } catch (Exception e) {
            logger.error("Failed to register Prometheus designer scripting functions", e);
        }
    }
}