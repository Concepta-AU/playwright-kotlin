plugins {
    kotlin("jvm") version "2.4.10"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0" apply false
}

repositories {
    mavenCentral()
}

dependencies {
    // test API (page objects, utility code) is treated as main code here, so this needs to be in `implementation` scope
    implementation("au.concepta:playwright-kotlin:0.8.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
