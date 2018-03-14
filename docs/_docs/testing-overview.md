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

## Matching Props

Once you know how to test for sub-components, you can learn about more advanced
techniques for verifying props on them in the [Matching
Props](/docs/prop-matching) guide.

## Testing Event Handlers

Event handlers are just like any other prop you would set on your component.
That means you can use the mechanisms you've learned before with Sub-Component
Testing. Find an example on how to use this in the
[Event Handler Testing](/docs/event-handler-testing) guide.

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
[Follow the guide](/docs/tests-in-android-studio) to get set up.


