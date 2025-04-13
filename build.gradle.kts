import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.1.20"
    `java-library`
    `maven-publish`
    signing
    id("com.vanniktech.maven.publish") version "0.31.0"
    id("org.jetbrains.dokka") version "2.0.0"
}

group = "au.concepta"

val junitVersion = "5.12.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.microsoft.playwright:playwright:1.51.0")

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

// see https://vanniktech.github.io/gradle-maven-publish-plugin/central/ on how to configure the properties for this
mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka("dokkaGenerate"),
            sourcesJar = true,
        )
    )

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    coordinates(project.group.toString(), rootProject.name, project.version.toString())

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
