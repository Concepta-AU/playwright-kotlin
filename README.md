![Mainline Build](https://github.com/Concepta-AU/playwright-kotlin/actions/workflows/build.yml/badge.svg?branch=main)
[<img src="https://img.shields.io/maven-central/v/au.concepta/playwright-kotlin">](https://central.sonatype.com/artifact/au.concepta/playwright-kotlin)

Kotlin API for Playwright Java
==============================

This project provides utility classes and a framework to build end-to-end (E2E) tests using the
[Playwright](https://playwright.dev/) Java runner with [Kotlin](https://kotlinlang.org/),
[jUnit 5](https://junit.org/junit5/), and [Gradle](https://gradle.org/).

Documentation
-------------

| Document | Description |
|---|---|
| [Getting Started](docs/getting-started.md) | Dependency setup, the Application / ApplicationPage / TestBase pattern, and extension functions |
| [Running Tests](docs/running-tests.md) | Test execution, environment variables, traces, and accessibility checks |

Find the latest version on [Maven Central](https://central.sonatype.com/artifact/au.concepta/playwright-kotlin).

Purpose
-------

The functionality added by this project includes:

 * establish patterns of using [Page Object Models](https://playwright.dev/docs/pom) and higher level application
   abstractions
 * manage creation of trace files and video recordings
 * automatically check for console and page errors when running tests, while tolerating (and reporting) transient
   transport errors, so a network hiccup does not fail an unrelated test
 * document usage patterns that have proven successful in practice
 * provide a set of extension functions to make the Playwright API more concise

Why Kotlin
----------

Using Kotlin as the language for the tests adds a number of features that are particularly useful for tests:

 * [named and defaultable arguments](https://kotlinlang.org/docs/functions.html) — easy test data generation with sensible defaults
 * [data classes](https://kotlinlang.org/docs/data-classes.html) — concise domain objects and page data models
 * [extension functions](https://kotlinlang.org/docs/extensions.html) — add custom interactions to `Page`, `Locator`, and `Frame`
 * [type-safe builders](https://kotlinlang.org/docs/type-safe-builders.html) — create DSLs for application interactions
 * strong type system with full IDE support (completion, compile-time error detection)
 * full access to the Java ecosystem (assertion libraries, fakers, date/time handling, etc.)

Concise API Example
-------------------

The extension functions replace verbose Java options objects with named, optional parameters:

```kotlin
// Standard Playwright: page.getByText("text", Page.GetByTextOptions().setExact(true))
page.getByText("text", exact = true)
```
