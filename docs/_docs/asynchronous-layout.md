---
docid: asynchronous-layout
title: Asynchronous Layout
layout: docs
permalink: /docs/asynchronous-layout
---

## Immutability and thread safety

Most issues with thread safety derive from concurrent reads and writes on mutable objects. This is how a classic example of this problem looks in Java:

``` java
  public class SomeExampleClass {
    private int mCounter;

    public String getThisOrThat() {
      if (mCounter > 10) {
        return "this":
      } else {
        mCounter++;
        return "that";
      }
    }
  }

```
If multiple threads were to invoke `getThisOrThat` on a shared instance of `SomeExampleClass` that would be the most classic example of race condition. By the time the second thread entering the method tries to read `mCounter`, the first thread might be in the process of executing `mCounter++` and we wouldn't be able to determine what's the value that the second thread would actually read from `mCounter`.
The problem in general is that in this code there is a mutable state (`mCounter`) and multiple threads trying to write and read it.
Race conditions are the most common problem when writing applications that try to distribute work on multiple threads.

This is exactly why traditionally, running UI code on multiple thread has always been extremely complex.
Android views are stateful and mutable. A `TextView` for example, has to keep track of the current text that it's displaying and at the same time exposes a `setText()` method that allows the developer to mutate the text.
This means that if the Android UI framework decided to offload things like layout calculation on a secondary thread, it would have to solve the problem of a user calling `setText()` from another thread and mutating the current text while
the layout computation is happening.

Let's go back for a second to our sample code. We said that the main problem there was the mutable state `mCounter` accessed inside our `getThisOrThat()` method. Is there a way to write code that is functionally equivalent without having to rely on such mutable state?
Let's try to imagine for a moment that no object can ever mutate its content after creation. If nothing can mutate, we can't ever have races between threads trying to mutate and read the same state.
We can rewrite our sample code to look like this:

``` java

  public static class Result {
    public final int mCounter;
    public final String mValue;

    public Result(int counter, int value) {
      mCounter = counter;
      mValue = value;
    }
  }

  public class SomeExampleClass {
    public static Result getThisOrThat(int counterValue) {
      if (counterValue > 10) {
        return new Result(counterValue, "this"):
      } else {
        return new Result(counterValue + 1, "that");
      }
    }
  }

```

Our method is now completely thread safe as it never modifies any internal state of `SomeExampleClass`. In this example `getThisOrThat()` is what's called a 'pure function' as its result only depends on the inputs and it doesn't have any side effect.

In Litho we try to apply exactly the same concepts to layout computation. A `Component` is an immutable object that contains all the inputs for the layout function in form of `@Prop` and `@State` values. This also explains why we need `@Prop` and `@State` to be immutable. If they weren't, we would lose the property of having layout as a 'pure function'.

Immutability in Java usually comes at the cost of having to do many more allocations. Even in our simple example we are now allocating a `Result` object for every invocation of our function. Litho uses pooling and [code generation](/docs/codegen) to minimize object allocations for you automatically.

## Sync and Async operations

Litho offers both synchronous and asynchronous APIs for layout computation. Both APIs are thread safe and can be invoked from any thread. The final layout will always represent the Component that was set last with calls to `setRoot()` or `setRootAsync()`.

Synchronous layout calculation ensures that immediately after calling `setRoot()` on a [ComponentTree](/javadoc/com/facebook/litho/ComponentTree), the result of the layout calculation is available to be mounted on a [LithoView](/javadoc/com/facebook/litho/LithoView).  
The main disadvantage of this is that the computation happens on the caller thread, therefore calling `setRoot()` from the main thread is not advisable. On the other hand there are situations in which you cannot wait for a background thread to compute a layout before showing something on screen, for example when the item you want to display is already in the viewport. In these cases calling `setRoot()` synchronously is the best route.
Having synchronous operations also makes it very easy to integrate Litho with pre-existing threading models. If your application already has a complex and structured thread design you might want to fit the layout calculation into it without relying on Litho's built-in threads.

Asynchronous layout calculation will use Litho's Layout Thread to compute the Component layout. This means that the work gets enqueued immediately on a separate thread and the layout result will not be visible immediately on the calling thread. Asynchronous layout operations are used widely for example from the [RecyclerBinder](/docs/recycler-component).
