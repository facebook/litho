---
docid: repo-structure
title: Repository Structure
layout: docs
permalink: /docs/repo-structure
---

This is a quick breakdown of what is where in the repository.

### `/docs/`

This directory holds the Jekyll files for the GitHub pages you are now reading.

### `/lib/`

A number of external libraries can be found in this sub-folder.  Roughly, they can be split into two categories

 * **Fetched libraries**:  These libraries are hosted on jCenter.  The corresponding sub-folder of `/lib/` will only contain a `BUCK` file, with the command to fetch the library.
 * **Bundled libraries**:  These libraries are included in their completeness.  This is the [proper buck way of doing things](https://buckbuild.com/command/fetch).  However, they vastly increase the size of the repository and are thus only included when absolutely necessary.

### `/sample-barebones/`

The source for the finished product of the [barebones tutorial](/tutorial/) is found here.  If you change the tutorial, you must update the code here.

### `/sample/`

Under this directory is found code for the Litho sample app.  This includes the playground, which you should use for all testing/bug reporting.

### `/litho-*/`

Litho is split into several sub-project so end users can pick and choose the
parts of the framework they want to use. The available projects are as follows:

 * `litho-annotation` is a pure Java library containing the annotations necessary to use the processor with.
 * `litho-core` contains the core framework code.
 * `litho-fresco` contains components for the use with the Fresco image library.
 * `litho-it` contains integration tests for the framework. It is necessary to have a separate project for this as it to avoid circular dependencies.
 * `litho-processor` contains the stand-alone annotation processor.
 * `litho-stetho` contains Stetho integrations for easier debugging and development.
 * `litho-stubs` contains stubbed out Android framework classes that are needed for some display list magic in `litho-core`.
 * `litho-testing` contains utilities to test Litho components.
 * `litho-widget` contains several mount specs for commonly used Android widgets.

### `/COMPONENTS_DEFS` and `/BUCK`

These files define how to build Litho.  The `BUCK` file is the input to [buck](https://buckbuild.com), and the `/COMPONENTS_DEFS` file contains some constants needed for buck to find targets inside the repository.  It is imported in `/BUCK`.

