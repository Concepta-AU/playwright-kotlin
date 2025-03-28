![Mainline Build](https://github.com/Concepta-AU/playwright-kotlin/actions/workflows/build.yml/badge.svg?branch=main)

Kotlin API for Playwright Java
==============================

This project contains a number of utility classes and a framework to build end-to-end (E2E) tests using:

 * the [Playwright](https://playwright.dev/) web runner (Java version)
 * [Kotlin](https://kotlinlang.org/) as programming language
 * [jUnit 5](https://junit.org/junit5/) as test runner
 * [Gradle](https://gradle.org/) as build tool

The following assumes you are familiar with Playwright, otherwise it is recommended to read through 
[its feature list](https://playwright.dev/) prior to continuing.

Using Kotlin as the language for the tests adds a number of features that are particularly useful for tests:

 * the combination of [named and defaultable arguments](https://kotlinlang.org/docs/functions.html) allows easy 
   generation of test data. For example: if a test needs to create a new user, all arguments such as first name, last
   name and email address can be defaulted to a value provided by a faker - any test that needs to use a particular
   value can then set only the value relevant. This makes it easier to write, but also easier to read: if the test
   sets only the email address, then it cares about the email address and none of the other values.
 * [data classes](https://kotlinlang.org/docs/data-classes.html) make it easy to create object describing the domain
   (such as users) or parts of a page being read (such as the rows of a data table).
 * [extension functions](https://kotlinlang.org/docs/extensions.html) allow for convenient additions to standard object
   such as expanding date/time objects with custom render functions to match UI behaviour, or adding custom interactions
   to the Playwright `Page` and `Locator` to handle behaviour specific to a UI library or the application under test.
 * [type-safe builders](https://kotlinlang.org/docs/type-safe-builders.html) allow creating simple domain specific
   languages (DSLs) to describe the interactions with the application under test.
 * due to the comparatively strong type system and availability of IDEs, writing Kotlin code can rely on effective
   support in code editors, from method completion to detection of errors at time of writing.
 * apart from the Kotlin standard libraries, the large ecosystem of Java libraries is available for use, including
   test assertion tooling, fakers, date/time handling and other functionality that can help writing better tests.

Gradle and jUnit are chosen as the standard and very stable tools in the Kotlin space.

The functionality added by this project includes:

 * establish patterns of using [Page Object Models](https://playwright.dev/docs/pom) and higher level application
   abstractions
 * manage creation of trace files and video recordings
 * automatically check for console and page errors when running tests
 * document usage patterns that have proven successful in practice
