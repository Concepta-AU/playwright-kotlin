import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar

repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version "2.4.10"
    `java-library`
    `maven-publish`
    signing
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("org.jetbrains.dokka") version "2.2.0"
    id("io.github.ben-manes.versions") version "0.56.0"
}

group = "au.concepta"

val junitVersion = "5.14.0"

dependencies {
    api("com.microsoft.playwright:playwright:1.61.0")
    api("com.deque.html.axe-core:playwright:4.12.0")

    // We use these as `api` as our main code is a testing framework itself
    api(kotlin("test"))
    api("org.junit.jupiter:junit-jupiter-api:$junitVersion")

    testImplementation("org.assertj:assertj-core:3.27.7")
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

kotlin {
    jvmToolchain(21)
}

fun isPrerelease(candidate: ModuleComponentIdentifier): Boolean {
    val lcVersion = candidate.version.lowercase()
    return lcVersion.contains("-alpha") ||
            lcVersion.contains("-beta") ||
            lcVersion.matches(".*-rc\\d*".toRegex()) ||
            lcVersion.matches(".*-m\\d+".toRegex())
}

// https://github.com/ben-manes/gradle-versions-plugin
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isPrerelease(candidate) ||
                // not doing jUnit 6 just yet
                candidate.group == "org.junit.jupiter" || candidate.version.startsWith("6.0")
    }
}

// https://vanniktech.github.io/gradle-maven-publish-plugin/central/
mavenPublishing {
    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka("dokkaGenerate"),
            sourcesJar = SourcesJar.Sources(),
        )
    )

    publishToMavenCentral(automaticRelease = true)
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
