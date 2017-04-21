---
docid: tutorial
title: Tutorial
layout: docs
permalink: /docs/tutorial
---

This tutorial assumes you've gone through the [Getting Started](getting-started) guide to set up Litho.

In this tutorial, you'll start by building a basic "Hello World!" screen using Litho and work your way up to creating a list of "Hello World!" items on the screen. Along the way, you'll learn about the building block of Litho: [Component](/javadoc/com/facebook/litho/Component) and [LithoView](/javadoc/com/facebook/litho/LithoView). You'll learn how to set properties on components.


## 1. Hello World

In this initial step, you'll display a view with "Hello World!".

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

**Extra:** If you want to debug your components hierarchy, you can also setup [Stetho](/docs/debugging#stetho) for your application in this step.

Next, add a predefined [Text](/javadoc/com/facebook/litho/widget/Text) Litho component to an activity:

```java
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext context = new ComponentContext(this);

    final Component component = Text.create(context)
        .text("Hello World")
        .textSizeDip(50)
        .build();

    setContentView(LithoView.create(context, component));
}
```

`LithoView` is an Android `ViewGroup` that can render components; it is the bridge between Litho components and Android `View`s. The example sets the content for the activity to a `LithoView` that displays a `Text` component.

How do the components come into play? Let's zero in on this piece of code:

```java
Text.create(context)
    .text("Hello World")
    .textSizeDip(50)
    .build();
```

`Text` is a core component defined in `com.facebook.litho.widget`.  It has a number of properties such as _text_ and _textSize_ which you can set as shown. These properties are called *props* as inspired by [React](https://facebook.github.io/react/) terminology.  
You'll learn how to write your own components later but it's worth noting the `Text` class is generated from a `TextSpec` class. The generated component class provides a builder API with methods to define values for the component's props.

The `Text` component is added as a single child component to the `LithoView` in the example. You could instead have a single root component with several child components. You'll see how to do this in follow-on examples.

That's it! Run the app, you should see something like this:

<img src="/static/images/barebones1.png" style="width: 300px;">

Not pretty, but this is certainly a start!

## 2. Your First Custom Component

At the end of this tutorial you'll have a simple, scrollable list. This list will just display an item with a title and subtitle a whole lot of times. Exciting times!   
In this part of the tutorial, you'll write a simple component that is the list item. Naturally, a real world app would define a more complicated component but you'll learn all the basics you need to do that in this example.

Ready? It's time to dive in and build this component. In Litho, you write *Spec* classes to declare the layout for your components. The framework then generates the underlying component class that you use in your code to create a component instance.

Your custom component will be called `ListItem` and it will display a title with a smaller subtitle underneath. Therefore, you need to create a class named `ListItemSpec` with the following content:

```java
@LayoutSpec
public class ListItemSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c) {
  
    return Column.create(c)
        .paddingDip(ALL, 16)
        .backgroundColor(Color.WHITE)
        .child(
            Text.create(c)
                .text("Hello world")
                .textSizeSp(40))
        .child(
            Text.create(c)
                .text("Litho tutorial")
                .textSizeSp(20))
        .build();
  }
}
```

You should recognize the `Text` component from the previous tutorial step. In this example, you're passing it in as a `child` property of a [Column](/javadoc/com/facebook/litho/Column). You can think of a `Column` as equivalent to a `<div>` in HTML.  It's a wrapper, used mainly for collating things together and perhaps adding some background styling.  Since Litho uses [Yoga](https://facebook.github.io/yoga/), you can add flexbox attributes to set the layout for the children of a `Column` or a `Row`. Here, you simply set the padding and the background color.

How do you render this component? In your activity, simply change the `Component` definition to:

```java
final Component text = ListItem.create(context).build();
```

**Note:** That's `ListItem` you're using, not `ListItemSpec`.

Where did `ListItem` come from?  Where are `create` and `build` defined?  This is the magic of Litho _Specs_.

In the [Getting Started](http://fblitho.com/docs/getting-started) guide, we saw how to add the dependencies to your project to make Litho code generation work.
This runs an annotation processor over your code.  It looks for `FooSpec` class names and generates `Foo` classes with the same package as your spec. These classes will have all the methods required by Litho automatically filled in. In addition, based upon the specification, there will be extra methods (such as `Text`'s `.textSizeSp` or the `.backgroundColor` method of `Column`/`Row`) generated by annotation processor.

It's as simple as that. Run your app. You should see something like this:

<img src="/static/images/barebones2.png" style="width: 300px;">

## 3. Creating a List of Items

You can handle lists in Litho using the core [Recycler](/javadoc/com/facebook/litho/widget/Recycler) component.  This component is conceptually similar to the Android `RecyclerView`.  However, with Litho, all the layout is performed in a separate thread, resulting in a substantial performance boost.

In this part of the tutorial, you'll use a [RecyclerBinder](/javadoc/com/facebook/litho/widget/RecyclerBinder) to provide components to a `Recycler`, in the same way that a `LayoutManager` works hand in hand with an `Adapter` to provide `View`s to a `RecyclerView`.

First, in your activity, modify the `Component` definition as follows:

```java
final RecyclerBinder recyclerBinder = new RecyclerBinder(
    context,
    new LinearLayoutInfo(this, OrientationHelper.VERTICAL, false));

final Component component = Recycler.create(context)
    .binder(recyclerBinder)
    .build();
```

This code constructs a `RecyclerBinder` and attaches it to a `Recycler`. A new `RecyclerBinder` takes as constructor parameters a component context and layout info. 

You then create and pass in the `Recycler` component to the `LithoView`.

Now turn your focus to populating the binder with list items. Define a helper function in your activity to do this:

```java
private static void addContent(RecyclerBinder recyclerBinder, ComponentContext context) {
    for (int i = 0; i < 32; i++) {
      recyclerBinder.insertItemAt(
          i,
          ComponentInfo.create()
              .component(ListItem.create(context).build())
              .build());
    }
}    
```

In the code, a [ComponentInfo](/javadoc/com/facebook/litho/ComponentInfo) is created that describes the components to be rendered by a `Recycler`. In this example, a `ListItem` is the component to be rendered.

Finally, make a call to `addContent` in your activity's `onCreate` method, after the `component` definition:

```java
addContent(recyclerBinder, context);
```

Run the app. You should see a scrollable list of 32 ListItem components:

<img src="/static/images/barebones3.png" style="width: 300px;">

## 4. Defining a Component's properties

Lists are no good if they only contain repetitive copies of a single component. In this part, you'll look at _properties_ or props. These are attributes you can set on components to change their behavior or appearance.

Adding props to a component is very simple. Props are parameters to methods of the component specification, annotated with the `@Prop` annotation.

Modify `ListItemSpec` as follows:

```java
@OnCreateLayout
static ComponentLayout onCreateLayout(
    ComponentContext c,
    @Prop int color,
    @Prop String title,
    @Prop String subtitle) {
    
  return Column.create(c)
        .paddingDip(ALL, 16)
        .backgroundColor(color)
        .child(
            Text.create(c)
                .text(title)
                .textSizeSp(40))
        .child(
            Text.create(c)
                .text(subtitle)
                .textSizeSp(20))
        .build();
}
```

This adds three props: `title`, `subtitle` and `color` props. Notice that the background color and the strings for the `Text` components' text are no longer hard-coded and are now based on the `onCreateLayout` method parameters.

The magic happens in the `@Prop` annotations and the annotation processor.  The processor generates methods on the component builder that correspond to the props in a smart way. You can now change the binder's construction of the component to:

```java
private void addContent(
    RecyclerBinder recyclerBinder, 
    ComponentContext context) {
  for (int i = 0; i < 32; i++) {
    ComponentInfo.Builder componentInfoBuilder = ComponentInfo.create();
    componentInfoBuilder.component(
        ListItem.create(context)
            .color(i % 2 == 0 ? Color.WHITE : Color.LTGRAY)
            .title("Hello, world!")
            .subtitle("Litho tutorial")
            .build());
    recyclerBinder.insertItemAt(i, componentInfoBuilder.build());
  }
}
```

Now when `ListItem` is constructed, the `color`, `title` and `subtitle` props are passed in with the background color alternating for each row.

Run the app. You should see something like this:

<img src="/static/images/barebones4.png" style="width: 300px;">

You can specify more options to the `@Prop` annotation.  For example, consider the property:

```java
@Prop(optional = true, resType = ResType.DIMEN_OFFSET) int shadowRadius,
```

This tells the annotation processor to construct a number of functions, such as `shadowRadiusPx`, `shadowRadiusDip`, `shadowRadiusSp` as well as `shadowRadiusRes`.

Congratulations on completing this tutorial! This basic tutorial should arm you with all the building blocks to start using Litho and building your own components. You can find the predefined widget components you can use in the [com.facebook.litho.widgets](/javadoc/com/facebook/litho/widget/package-frame) package.
You can find the [completed tutorial here](https://github.com/facebook/litho/tree/master/sample-barebones). Be sure to check out [this sample](https://github.com/facebook/litho/tree/master/sample) for more in-depth code as well as the Litho API documentation.
