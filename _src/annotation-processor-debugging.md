---
id: annotation-processor-debugging
title: Annotation Processor Debugging
---

Annotation Processor debugging is a three-step process:

1. [Run the Compiler in Debug Mode](#1-run-the-compiler-in-debug-mode)
2. [Set Breakpoints in the IDE](#2-set-breakpoints-in-the-ide)
3. [Attach the Debugger to the Build Process](#3-attach-the-debugger-to-the-build-process)

## 1. Run the Compiler in Debug Mode

### Using Gradle

Debugging the Annotation Processor stage is troublesome as you are essentially debugging the Java Compiler (`javac`). Gradle overcomes this issue by enabling you to attach a debugger, during the compilation stage, by setting the flag `org.gradle.debug` to true.

To use that flag, determine a target that invokes the annotation processor and start it,  as shown in the following `litho-widget` example:

```sh
./gradlew --no-daemon -Dorg.gradle.debug=true :litho-widget:compileDebugJavaWithJavac
```

This will pause the execution during the *Starting Daemon* phase until you connect your debugger.

### Using Buck

Buck doesn't have built-in support for suspending the compiler.  However, you can attach a debugger by directly instrumenting the Java Virtual Machine (JVM) to open a debugging port.

For example, to trigger the Annotation Processor from the Sample App, use the following:

```bash
BUCK_DEBUG_MODE=1 NO_BUCKD=1 buck build //fbandroid/libraries/components/sample/src/main/java/com/facebook/samples/litho:litho
```

## 2. Set Breakpoints in the IDE

If you are not sure where to put breakpoints, the following classes might be a good start:

* [BuilderGenerator](https://github.com/facebook/litho/blob/master/litho-processor/src/main/java/com/facebook/litho/specmodels/generator/BuilderGenerator.java) - generates the builder for a Component.
* [ComponentBodyGenerator](https://github.com/facebook/litho/blob/master/litho-processor/src/main/java/com/facebook/litho/specmodels/generator/ComponentBodyGenerator.java) - generates the implementation of a Component.

:::note
Breakpoints won't get hit if there are no code changes as Buck uses a cached built output; either call `buck clean` or make some changes in code.
:::

## 3. Attach the Debugger to the Build Process

There are two options to attach the debugger: the [Attach to process Menu Option](#attach-to-process) menu option or [Create a new debug configuration](#create-a-new-debug-configuration).

### Attach to Process Menu Option

1. Run -> Attach To Process - **don't pick the 'Attach Debugger to Android Process' option!**
2. Wait for Android Studio to find the waiting Buck or Gradle process on the respective port (the same one used while creating the configuration).
3. Select that option from the choices (see the following screenshot).

![Attach to Process](/images/debugging-attach-to-process.png)

### Create a new Debug Configuration

1. Run -> Edit Configurations -> Add -> Remote
2. Give it a name, such as 'Debug Gradle' or 'Debug Buck' (see the following screenshot)
3. Debugger mode: Attach to remote JVM.
4. Host: localhost.
5. Port: 5005 (use the port number printed when running Gradle\Buck in debug mode in the previous step. Default: Gradle = 5005, Buck = 8888).
6. Click 'OK' to create it.

![IntelliJ Remote Target](/images/remote-debugger.png)

7. Select the new configuration from the Configuration dropdown.
8. Click the 'Debug' Button.
