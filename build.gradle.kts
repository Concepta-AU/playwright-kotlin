plugins {
    kotlin("jvm") version "2.1.10"
}

group = "au.concepta"
version = "1.0-SNAPSHOT"

val junitVersion = "5.12.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.microsoft.playwright:playwright:1.50.0")

    // We use these as `implementation` as our main code is a testing framework itself
    implementation(kotlin("test"))
    implementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

kotlin {
    jvmToolchain(21)
}
