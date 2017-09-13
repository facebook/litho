---
docid: debugging
title: Debugging
layout: docs
permalink: /docs/debugging
---

## Stetho

[Stetho](http://facebook.github.io/stetho/) is a great debugging tool for Android and we have made sure it works with Litho as well.
To enable Litho debugging in Stetho, add the following lines in the `onCreate()` method of your `Application` implementation.

```java
public class SampleApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, false);
    
    Stetho.initialize(
        Stetho.newInitializerBuilder(this)
              .enableWebKitInspector(new LithoWebKitInspector(this))
              .build());
  }
}
```

This will enable full integration of Litho in stetho. After you have enabled Litho support, just start your app and navigate to `chrome://inspect` in your browser.

![Stetho start](/static/images/stetho-start.png)

Click on the inspect link for the application you would like to inspect (we are using the Litho sample app). This opens a UI inspector where you are able to inspect the `View` and `Component` hierarchy of your application.

![Stetho inspect](/static/images/stetho-inspect.png)

When inspecting a Litho component you are also given the ability to edit the contents of your UI directly from the inspector! This enables quick design iterations by tweaking margins, padding, and many other properties, without needed to re-compile or re-start the application. You can also use this to quickly test that your UI handles different lengths of text properly.

![Stetho edit](/static/images/stetho-edit.png)


## Annotation Processor

**With Gradle**

Debugging the annotation processor stage is a bit tricky as you are essentially debugging the Java Compiler (`javac`).
Gradle, however, allows you to attach a debugger during the compilation stage by setting the property `org.gradle.debug` to true.
To start Gradle with that flag, figure out a target that invokes the annotation processor and start it like this:

```
./gradlew --no-daemon -Dorg.gradle.debug=true :litho-widget:compileDebugJavaWithJavac
```

In this case we are compiling the `litho-widget` package.

This will pause the execution during the "Starting Daemon" phase until you connect your debugger. This can be done by
running a standard "Remote" target in IntelliJ which will look like this:

![IntelliJ Remote Target](/static/images/remote-debugger.png)

Now set a breakpoint at the right point, run your remote target and debug away!

**With Buck**

Buck doesn't have built-in support for suspending the compiler, but you can still attach a debugger by directly
instrumenting the JVM to open a debugging port.

For that, first make sure to kill any previous instances of Buck which may still be running with old flags, then
set the `JAVA_TOOLS_OPTIONS` to contain the JDWP options.

```
$ buck kill
$ env JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" buck build //litho-widget/...
```

Note that this won't pause the JVM (`suspend=n`), but as the process will continue to run you can choose to
attach to the process at any time.
