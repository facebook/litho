---
docid: tutorial
title: Tutorial
layout: docs
permalink: /docs/tutorial
---


This tutorial assumes you've gone through the [Getting Started](getting-started.html) guide to set up Litho.

In this tutorial, you'll start by building a basic "Hello World!" screen using Litho and work your way up to creating a list of "Hello World!" items on the screen. Along the way, you'll learn about the building blocks of Litho: components, `ComponentTree`, and `ComponentView`. You'll learn how to set properties on components.


## 1. Hello World

In this inital step, you'll display a view with "Hello World!".

First, initialize `SoLoader` in your `Application` class:

```java
public class SampleApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, false);
  }
}
```

Behind the scenes, Litho uses [Yoga](https://facebook.github.io/yoga/) for layout. Yoga has native dependencies and [SoLoader](https://github.com/facebook/SoLoader) is brought in to take care of loading those. Initializing `SoLoader` here ensures that you're not referencing unloaded libraries later on.

Next, add a `Text` Litho component to an activity:

``` java
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentView componentView = new ComponentView(this);
    final ComponentContext context = new ComponentContext(this);

    final Component text = Text.create(context)
            .text("Hello World")
            .textSizeDip(50)
            .build();
    final ComponentTree componentTree = ComponentTree.create(context, text).build();

    componentView.setComponent(componentTree);
    setContentView(componentView);
}
```

The two main players when using components are `ComponentView` and `ComponentTree`. A `ComponentView` is an Android `ViewGroup` that can render components. The example sets the content for the activity to a `ComponentView`. A `ComponentTree` represents a tree of components. You attach a `ComponentTree` to a `ComponentView` to manage the life cycles of the components in a thread-safe way.

How do the components come into play? Let's zero in on this piece of code:

``` java
Text.create(context)
    .text("Hello World")
    .textSizeDip(50)
    .build();
```

`Text` is a core component defined in `com.facebook.litho.widget`.  It has a number of _properties_ such as _text_ and _textSize_ which you can set as shown. These _properties_ are called props to mimic [React](https://facebook.github.io/react/) terminology. You'll learn how to write your own components later but it's worth noting the `Text` class is generated from a `TextSpec` class. The generated component class provides a builder API with methods to define values for the component's props.
 
The `Text` component is added as a single child component to the `ComponentTree` in the example. You could instead have a single root component with several child components. You'll see how to do this in follow-on examples.

Note that often this would be written as the more compact and pleasing, but slightly more confusing

That's it! Run the app, you should see something like this:

<img src="/static/images/barebones1.png" style="width: 300px;">

Not pretty, but this is certainly a start!

## 3. Your First Custom Component

At the end of this tutorial you'll have a simple, scrollable feed. This feed will just say "Hello World" a whole lot of times. Exciting times! In this section, you'll define one of the elements, a "Hello World" component that appears in the feed. Naturally, a real world app would define a more complicated component but you'll learn all the basics you need to do that in this example.

Ready? It's time to dive in and build this component. In Litho, a component is defined by _Spec_ classes. The framework then generates the underlying component class that you use in your code to create a component instance.

Your custom component will be called `FeedItem`. Therefore, define a class named `FeedItemSpec` with the following content:

``` java
@LayoutSpec
public class FeedItemSpec {
  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c) {
    return Container.create(c)
        .paddingDip(ALL, 16)
        .backgroundColor(Color.WHITE)
        .child(
            Text.create(c)
                .text("Hello World")
                .textSizeSp(40))
        .build();
  }
}
```

You should recognize the `Text` component from the previous tutorial step. In this example, you're passing it in as a "child" property of a `Container`. You can think `Container`s like `<div>`s in HTML.  It's a wrapper, used mainly for collating things together and perhaps adding some background styling.  Since Litho uses [Yoga](https://facebook.github.io/yoga/), you can add flexbox attributes to set the layout for the children of a `Container`. Here, you simply set the padding and the background color.

How do you this component? In your activity, simply change the `ComponentTree` definition to:

``` java
final ComponentTree componentTree = ComponentTree.create(
    context,
    FeedItem.create(context))
        .build();
```

**Note:** That's `FeedItem` you're using, not `FeedItemSpec`.

Where did `FeedItem` come from?  Where are `create` and `build` defined?  This is the magic of Litho _Specs_. 

??? HOW TO DO THIS WITHOUT BUCK ???

You need to add to the buck target `/src/main/java/com/company/tutorial:tutorial`

``` python
plugins = [
        INSERT_ANNOTATION_TARGET_HERE,
    ],
```

This runs an annotation processor over your code.  It looks for `(.*)Spec` class names and constructs `(.*)` classes.  These classes will have all the methods required by Litho automatically filled in.  In addition, based upon the specification, there will be extra methods (such as `Text`'s `.textSizeSp` or the `.backgroundColor` method of `Container`).

It's as simple as that. Run your app. You should see something like this:

<img src="/static/images/barebones2.png" style="width: 300px;">

## 4. Creating a List of Items

You can handle lists in Litho using the core `Recycler` component.  This component is conceptually similar to the Android `RecyclerView`.  However, with Litho, all the layout is performed in a separate thread, resulting in a substantial performance boost.

In this part of the tutorial, you'll use a `RecyclerBinder` to provide components to a `Recycler`, in the same way that a `LayoutMangager` works hand in hand with an `Adapter` to provide `View`s to a `RecyclerView`.

First, you need to add the Android support `RecyclerView` library to your gradle dependencies:

```java
dependencies {
  // ...
  compile 'com.android.support:recyclerview-v7:+'
}
```

Next, in your activity modify the `ComponentTree` definition as follows:

``` java
final RecyclerBinder recyclerBinder = new RecyclerBinder(
    context,
    4.0f,
    new LinearLayoutInfo(this, OrientationHelper.VERTICAL, false));

final ComponentTree componentTree = ComponentTree.create(
    context,
    Recycler.create(context)
        .binder(recyclerBinder))
    .build();
```

This code constructs a `RecyclerBinder` and attaches it to a `Recycler`. A new `RecyclerBinder` takes as constructor parameters a component context, a range ratio, and layout info. In your example, this sets the range ratio to 4 and a `LinearLayoutInfo` for the layout info.

You then create and pass in the a `Recycler` component to the component tree.

Now turn your focus to populating the binder. Define a helper function in your activity to do this:

``` java
private void addContent(RecyclerBinder recyclerBinder, ComponentContext context) {
  for (int i = 0; i < 32; i++) {
    ComponentInfo.Builder componentInfoBuilder = ComponentInfo.create();
    componentInfoBuilder.component(FeedItem.create(context).build());
    recyclerBinder.insertItemAt(i, componentInfoBuilder.build());
  }
}
```

In the code, a `ComponentInfo` is created that describes the components to be rendered by a `Recycler`. In this example, a `FeedItem` is the component to be rendered.

Finally, make a call to `addContent` in your activity's `onCreate` method, after the `componentTree` definition:

```java
addContent(recyclerBinder, context);
```

Run the app. You should see a scrollable list of 32 "Hello World" components:

<img src="/static/images/barebones3.png" style="width: 300px;">

## 5. Defining a Component's properties

Feeds are no good if they only contain repetitive copies of a single component. In this part, you'll look at _properties_ or props. These are attributes you can set on components to change their behavior or appearance.

Adding props to a component is very simple. Props are parameters to methods of the component specification, annotated with the `@Prop` annotation.

Modify `FeedItemSpec` as follows:

``` java
@OnCreateLayout
static ComponentLayout onCreateLayout(
    ComponentContext c,
    @Prop int color,
    @Prop String message) {
  return Container.create(c)
      .paddingDip(ALL, 16)
      .backgroundColor(color)
      .child(
          Text.create(c)
              .text(message)
              .textSizeSp(40))
      .build();
}
```

This adds two props: `message` and `color` props. Notice that the background color and `Text` component's text are no longer hard-coded and are now based on the `onCreateLayout` method parameters.

The magic happens in the `@Prop` annotations and the annotation processor.  The processor produces methods on the builder that correspond to the props in a smart way. You can now change the binder's construction of the component to:

``` java
private void addContent(RecyclerBinder recyclerBinder, ComponentContext context) {
  for (int i = 0; i < 32; i++) {
    ComponentInfo.Builder componentInfoBuilder = ComponentInfo.create();
    componentInfoBuilder.component(
        FeedItem.create(context)
            .color(i % 2 == 0 ? Color.WHITE : Color.LTGRAY)
            .message("Hello, world!")
            .build());
    recyclerBinder.insertItemAt(i, componentInfoBuilder.build());
  }
}
```

Now when `FeedItem` is constructed, the `color` and `message` props are passed in with the background color alternating for each row.

Run the app. You should see something like this:

<img src="/static/images/barebones4.png" style="width: 300px;">

You can specify more options to the `@Prop` annotation.  For example, consider the property:

``` java
@Prop(optional = true, resType = ResType.DIMEN_OFFSET) int shadowRadius,
```

This tells the annotation processor to construct a number of functions, such as `shadowRadiusPx`, `shadowRadiusDip`, `shadowRadiusSp` as well as `shadowRadiusRes`.

Congratulations on completing this tutorial! This basic tutorial should arm you with all the building blocks to start using Litho and building your own components. You can find the [completed tutorial here](https://github.com/facebook/litho/tree/master/sample-barebones). Be sure to check out [this sample](https://github.com/facebook/litho/tree/master/sample) for more in-depth code as well as the Litho API documentation.

