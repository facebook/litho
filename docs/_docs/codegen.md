---
docid: codegen
title: Code Generation
layout: docs
permalink: /docs/codegen
---

As explained in [Writing Components](/docs/writing-components), Litho relies on code generation in order to create *Components* from *Component Specs*. This process utilises intermediate *ComponentSpec* representations called [SpecModels](/javadoc/com/facebook/litho/specmodels/model/SpecModel), which are immutable java objects. 

Code generation comprises three main steps: 

- Creating a Spec Model from a Component Spec. 
- Spec Model Validation. 
- Component generation from a given Spec Model.

#### Spec Model Creation
Spec models are created at compile time using an annotation processor, which is a tool in javac for scanning and processing annotations. The Litho annotation processor will process the annotations, methods and fields on your Component Specs and create a Spec Model for each one. 

In the future, we will add the ability to create Spec Models in other ways. For example, we want to be able to create Spec Models directly in Android Studio/Intellij, which would allow us to generate Components without having to build the source code. 

#### Spec Model Validation
Spec Models have a method called `validate()`, which returns a list of [SpecModelValidationErrors](/javadoc/com/facebook/litho/specmodels/model/SpecModelValidationError). If this list is empty then the Spec is well-formed and may be used to generate a valid Component. If not, then it will contain a list of errors that need fixing up before a valid Component may be generated. 

#### Component Generation
If the validation step on a Spec Model is successful, then the `generate` method may be called. This will create a [Javapoet](https://github.com/square/javapoet) `TypeSpec` which can then be easily used to create a Component class file. 

#### Setting up Code Generation for your project
If you set up your project using the instructions in the [Getting Started](/docs/getting-started) section, then code generation will automatically take place on your project.
