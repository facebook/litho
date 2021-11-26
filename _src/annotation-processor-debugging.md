---
id: annotation-processor-debugging
title: Annotation Processor Debugging
---


## 1. Run compiler in debug mode

### With Gradle:
Debugging the annotation processor stage is a bit tricky as you are essentially debugging the Java Compiler (`javac`).
Gradle, however, allows you to attach a debugger during the compilation stage by setting the property `org.gradle.debug` to true.
To start Gradle with that flag, figure out a target that invokes the annotation processor and start it like this (example for `litho-widget` module):

```
./gradlew --no-daemon -Dorg.gradle.debug=true :litho-widget:compileDebugJavaWithJavac
```

This will pause the execution during the "Starting Daemon" phase until you connect your debugger.

### With Buck:

Buck doesn't have built-in support for suspending the compiler, but you can still attach a debugger by directly
instrumenting the JVM to open a debugging port.

For example, to trigger annotation processor from our Sample App:
```bash
BUCK_DEBUG_MODE=1 NO_BUCKD=1 buck build //fbandroid/libraries/components/sample/src/main/java/com/facebook/samples/litho:litho
```

## 2. Set breakpoints in IDE

  * If you are not sure where to put breakpoints, these classes might be a good start:
    * [BuilderGenerator](https://github.com/facebook/litho/blob/master/litho-processor/src/main/java/com/facebook/litho/specmodels/generator/BuilderGenerator.java) - Class that generates the builder for a Component
    * [ComponentBodyGenerator](https://github.com/facebook/litho/blob/master/litho-processor/src/main/java/com/facebook/litho/specmodels/generator/ComponentBodyGenerator.java) - Class that generates the implementation of a Component

:::note
Breakpoints won't get hit if there are no code changes as Buck will use cached built output. Please either call `buck clean` or make some changes in code
:::


## 3. Attach debugger to build process

There are 2 options here: using **Attach to process...** menu option or using **pre-configured Run Configuration**

### Attach to Process:
1. Run -> Attach To Process
2. Make sure to not pick **"Attach Debugger to Android Process"**!
3. Wait for Android Studio to find the waiting Buck or Gradle process on a respective port (the same one used in previous steps)
4. Select that option from the choices

![Attach to Process](/images/debugging-attach-to-process.png)

### Create a new Debug Configuration:
1. Run -> Edit Configurations -> Add -> Remote
2. Give it a name, ie: Debug Gradle/Buck
3. Debugger mode: Attach to remote JVM
4. Host: localhost
5. Port: 5005 (use the port number printed when running Gradle\Buck in debug mode in the previous step. By default: 5005 is for Gradle and 8888 for Buck)
6. Click OK to create it

![IntelliJ Remote Target](/images/remote-debugger.png)

7. Select the new configuration from the Configuration dropdown
8. Click Debug Button
