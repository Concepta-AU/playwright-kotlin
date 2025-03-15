plugins {
    kotlin("jvm") version "2.0.0"
}

val junitVersion = "5.10.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.microsoft.playwright:playwright:1.45.1")

    // We use these as `implementation` as our main code is a testing framework itself
    implementation(kotlin("test"))
    implementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.assertj:assertj-core:3.26.3")
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

kotlin {
    jvmToolchain(21)
}
