plugins {
    `java-library`
}

val ignitionTarget = rootProject.findProperty("ignitionTarget")?.toString() ?: "8.3"
val sdkVersion = if (ignitionTarget == "8.1") "8.1.44" else libs.versions.ignition.sdk.get()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets.main {
    java.srcDir(if (ignitionTarget == "8.1") "src/v81/java" else "src/v83/java")
}

dependencies {
    // Reference to our common module
    implementation(projects.common)

    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:$sdkVersion")
    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:$sdkVersion")

    // Bundle Prometheus Client with the module
    modlImplementation(libs.prometheus)
    modlImplementation(libs.prometheus.common)
    modlImplementation(libs.prometheus.dropwizard)

    // The Prometheus 0.16 servlet only works with javax.servlet, so it's only
    // bundled for the 8.1 target. The 8.3 target uses a custom jakarta servlet
    // that writes Prometheus text format directly.
    if (ignitionTarget == "8.1") {
        modlImplementation(libs.prometheus.servlet)
    }
}
