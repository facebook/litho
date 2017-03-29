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

### `/src/`

This directory contains all the source for Litho.  It has three sub folders.

 * `/src/main` contains all the core code for components, the annotation processor, widgets etc.
 * `/src/test` contains all the testing code that is to be run by a test runner (typically `buck test`)
 * `/src/debug` contains code for Stetho integration, as well as some helpers for the test suite

### `/COMPONENTS_DEFS` and `/BUCK`

These files define how to build Litho.  The `BUCK` file is the input to [buck](https://buckbuild.com), and the `/COMPONENTS_DEFS` file contains some constants needed for buck to find targets inside the repository.  It is imported in `/BUCK`.

