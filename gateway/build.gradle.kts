plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Reference to our common module
    implementation(projects.common)

    compileOnly(libs.ignition.common)
    compileOnly(libs.ignition.gateway.api)
    
    // Bundle Prometheus Client with the module
    modlImplementation(libs.prometheus)
    modlImplementation(libs.prometheus.servlet)
    modlImplementation(libs.prometheus.dropwizard)

}
