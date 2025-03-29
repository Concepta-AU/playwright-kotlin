plugins {
    kotlin("jvm") version "2.1.10"
    `java-library`
    `maven-publish`
    signing
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name = "Kotlin API for Playwright"
                description = "A framework and utilities for using the Playwright automation framework with Kotlin."
                url = "https://github.com/Concepta-AU/playwright-kotlin"
                licenses {
                    license {
                        name = "The Unlicense"
                        url = "https://unlicense.org/"
                    }
                }
                developers {
                    developer {
                        name = "Peter Becker"
                        email = "peter@concepta.au"
                        organization = "Concepta Consulting Pty Ltd"
                        organizationUrl = "https://concepta.au"
                    }
                }
                scm {
                    connection = "scm:git:git@github.com:Concepta-AU/playwright-kotlin.git"
                    developerConnection = "scm:git:ssh://github.com:Concepta-AU/playwright-kotlin.git"
                    url = "https://github.com/Concepta-AU/playwright-kotlin"
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
