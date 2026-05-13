plugins {
    `java-library`
}

val ignitionTarget = rootProject.findProperty("ignitionTarget")?.toString() ?: "8.3"
val sdkVersion = if (ignitionTarget == "8.1") "8.1.44" else libs.versions.ignition.sdk.get()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:$sdkVersion")
    compileOnly("com.inductiveautomation.ignitionsdk:designer-api:$sdkVersion")

    // Reference to our common module
    implementation(projects.common)
}
