plugins {
    alias(libs.plugins.ignition.module)
    id("eclipse") // Added for better IDE support with Eclipse & VS Code
}

// Determine the project version once and reuse it everywhere
val versionProp = project.findProperty("version")?.toString()
val projectVersion = if (versionProp.isNullOrBlank() || versionProp == "unspecified") "0.0.1-SNAPSHOT" else versionProp


allprojects {
    // Set the version for all projects. Used in artifact naming and module version
    // This will be overridden by -Pversion parameter if provided
    version = projectVersion

    // Apply the eclipse plugin to all projects for consistent IDE support
    apply(plugin = "eclipse")
}

extra["sdkVersion"] = "8.1.44"

ignitionModule {
    name.set("Prometheus Metrics Exporter")
    fileName.set("Prometheus-Exporter.modl")
    id.set("dev.bwdesigngroup.prometheus.PrometheusExporter")
    moduleVersion.set(projectVersion)
    moduleDescription.set("Adds Prometheus metrics exporting to Ignition")
    requiredIgnitionVersion.set(libs.versions.ignition.get())

    projectScopes.putAll(
        mapOf(
            ":common" to "GCD",
            ":gateway" to "G",
            ":designer" to "D", 
            ":client" to "C"
        )
    )

    hooks.putAll(
        mapOf(
            "dev.bwdesigngroup.prometheus.gateway.PrometheusExporterGatewayHook" to "G",
            "dev.bwdesigngroup.prometheus.designer.PrometheusDesignerHook" to "D",
            "dev.bwdesigngroup.prometheus.client.PrometheusClientHook" to "C"
        )
    )

    applyInductiveArtifactRepo.set(true)
    skipModlSigning.set(!findProperty("signModule").toString().toBoolean())
}

tasks.withType<io.ia.sdk.gradle.modl.task.Deploy>().configureEach {
    hostGateway.set(project.findProperty("hostGateway")?.toString() ?: "")
}

val deepClean by
    tasks.registering {
        dependsOn(allprojects.map { "${it.path}:clean" })
        description = "Executes clean tasks and remove node plugin caches."
        doLast { delete(file(".gradle")) }
    }