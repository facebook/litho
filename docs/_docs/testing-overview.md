---
docid: testing-overview
title: Testing Overview
layout: docs
permalink: /docs/testing-overview.html
---

Litho provides a variety of tools for automated testing. This page aims to
give a brief overview of the different concepts and show you where to go
to learn more.

## Unit Testing

Litho provides a suite of helpers to make unit testing easier. Learn about the
setup with our JUnit Rules and AssertJ helpers in [Unit Testing Basics](/docs/unit-testing).

## Sub-Component Testing

A common way to test components is by looking at the tree they generate and make
assertions over its children. [Sub-Component
Matching](/docs/subcomponent-testing) lays out the concepts and APIs for writing
declarative matchers against your component trees.

## Sections

For testing Sections, we have a few helpers, including
`SectionComponentTestHelper` and `SubSection` which are explained in detail
under [Unit Testing Sections](/docs/sections-testing).

## End-to-End Testing

We offer an optional package for writing end-to-end tests with Espresso. Learn
more in the [Espresso](/docs/espresso-testing) section.

## Android Studio

Lastly, if you want to run unit tests in Android Studio, you currently have to
jump through some hoops as loading native code is not well supported by the IDE.
[Follow the guide](/docs/test-in-android-studio) to get set up.


