---
docid: error-boundaries
title: Error Boundaries
layout: docs
permalink: /docs/error-boundaries
---

Litho provides a feature for handling errors raised inside Components inspired
by the eponymous feature in [React](https://reactjs.org/docs/error-boundaries.html).
It allows you to catch and handle errors higher up in the tree and provide
appropriate fallback, logging or retry mechanisms.

Prefer to jump straight into code? Our [sample
app](https://github.com/facebook/litho/tree/master/sample/src/main/java/com/facebook/samples/litho/errors)
contains a full example of using error boundaries in a Sections-powered list.

## Conceptual Overview

> NOTE: Error boundaries are still considered experimental and disabled by
> default. To use them, you have to enable `ComponentsConfiguration.enableOnErrorHandling`.
> The supported delegate methods are currently limited to:
> - `onCreateLayout`
> - `onCreateLayoutWithSizeSpec`
> - `onMount`
> We plan to expand them to more delegates in the future.

A component becomes an error boundary when it defines an
[`OnError`](/javadoc/com/facebook/litho/annotations/OnError.html) delegate method.
The method will receive all exceptions that occur are raised in supported
methods of components sitting underneath the error boundary in the tree.

```java
@LayoutSpec
public class ErrorBoundarySpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop Component child) {
    return child;
  }

  @OnError
  static void onError(ComponentContext c, Exception error) {
    Log.e("ErrorBoundary", "Exception caught at boundary.", error);

    if (safeToIgnore(error)) {
      return;
    } else {
      throw new RuntimeException(error);
    }
  }
}
```

This shows an example of an error boundary that wraps a child component
and swallows errors it regards as "safe" while always sending them to the log first.
In case that an exception is deemed unsafe, it is reraised as `RuntimeException`
and will likely crash the application.

But let's be very clear: Ignoring errors can be dangerous and if you know the
exceptions that can be raised inside a method, you should handle them locally.

## Providing Fallbacks

Especially during development and for debug builds, it can be very helpful to
provide error information instead of crashing the app for unexpected errors.
Let's expand the previous example and show an error message in place of the
wrapped component by using [State](/docs/state).

```java
@LayoutSpec
public class ErrorBoundarySpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop Component child,
      @State Optional<Exception> error) {

    if (error.isPresent()) {
      return Column.create(c)
          .marginDip(YogaEdge.ALL, 16)
          .child(
              Text.create(c)
                  .text("Error caught at boundary: " + error.get().getMessage())
                  .textColor(Color.RED)
                  .build())
          .build();
    }

    return child;
  }

```

Instead of simply returning the child, we now receive a state value for an
exception. If it is set, we instead return a Text component that displays
the error message.

```java
  @OnCreateInitialState
  static void createInitialState(
    ComponentContext c,
    StateValue<Optional<Exception>> error) {

    error.set(Optional.<Exception>empty());
  }

  @OnUpdateState
  static void updateError(
    StateValue<Optional<Exception>> error,
    @Param Exception e) {

    error.set(Optional.of(e));
  }
```

We set up a state value for the error object we receive in `@OnError`.

```java
  @OnError
  static void onError(ComponentContext c, Exception error) {
    ErrorBoundary.updateErrorAsync(c, error);
  }
}
```

Lastly, when the `onError` delegate gets called, we trigger an asynchronous
state update to the error. This will in turn rerender the component and run
`onCreateLayout` with the error value set.

Note that we use `updateErrorAsync` here as opposed to the synchronous variant.
This is important as crashes in MountSpecs can otherwise cause undefined
behavior.

## Re-raising Exceptions

An error boundary may choose to only handle certain classes of errors. For
example, you may want to introduce a `MediaRetryErrorBoundarySpec` for crashes
that arise from your media player and provide a mechanism to restart the
playback. You may, however, not want to handle errors that are related to a
commenting function.

You can re-raise an exception from within an `onError` delegate so that it
propagates up the component tree until it is either caught by another error
boundary or hits the root and causes a crash. This is done by calling
[`ComponentUtils.raise`](/javadoc/com/facebook/litho/ComponentUtils.html#raise-com.facebook.litho.ComponentContext-java.lang.Exception-) with your context and the exception.

```java
@OnError
static void onError(ComponentContext c, Exception error) {
  if (canHandle(error)) {
    ErrorBoundary.updateError(c, error);
  } else {
    ComponentUtils.raise(c, error);
  }
}
```

## Why not Try/Catch?

You may wonder why all this additional infrastructure would be necessary when
you could just use `try`/`catch`. It turns out that there is no easily
accessible place to wrap your user code like this.

The following example shows how **not** to do it:

```java
@OnCreateLayout
static Component onCreateLayout(ComponentContext c) {
  // This won't work.
  try {
    return PossiblyCrashingSubTitleComponent
        .create(c)
        .color(Color.RED)
        .build();
  } catch (Exception e) {
    return FallbackComponent.create(c).exception(e).build();
  }
}
```

Assuming that `PossiblyCrashingSubTitleComponentSpec` throws an exception in
`onCreateLayout` this would not be caught by this block. The reason for this is
that you are just returning a declaration of your layout here and don't actually
execute any code. This is the responsibility of the Litho framework, hence the
need to provide higher-level infrastructure to give you access to those errors.

## Limitations

- Error boundaries are a highly experimental feature. There are many ways in
  which errors can happen and there could be cases which we fail to correctly
  recover from, leaving Litho in an inconsistent state.

- Errors thrown in the error boundary itself are not caught but propagated
  further up the chain. For instance, if the `onCreateLayout` inside your
  `ErrorBoundarySpec` raises an exception, it won't be passed to the `onError`
  method of the same component.

- Do not use Error Boundaries for control flow. This should go without saying as
  this is sitting on top of exceptions which themselves shouldn't be used for
  control flow. Error boundaries exist to deal with the realities of application
  development and improve user experience, not to structure your applications
  around them.

- Not all delegate methods are supported yet. We plan to expand support in the
  future, so ensure that your error boundaries can handle exceptions from all
  component code.

- Exceptions that happen during state updates or in event handlers are not
  supported at the moment.
