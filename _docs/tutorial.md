---
docid: tutorial
title: Tutorial
layout: docs
permalink: /docs/tutorial.html
---

## 1. Getting an app off the ground

In this, the first of five basic parts of this tutorial in using Litho, we'll build a very simple app.  This app will be built with [buck](https://buckbuild.com), and use the [SoLoader](https://github.com/facebook/SoLoader) library.  Apart from that, though, it won't do anything.  The purpose of this tutorial is to set the stage for when we add Litho code, so that we don't have to clutter up with unrelated fluff.

**Requirements:** Buck set up with Android SDK.

Lets start with a super basic Android manifest in `src/main/AndroidManifest.xml`.  This manifest simply defines the application `SampleApplication` and the action `SampleActivity`, and makes it runnable.  We don't ask for any special permissions or specify any interesting names or icons.  This is a barebones tutorial!

``` xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.company.tutorial">

  <uses-sdk android:minSdkVersion="15" android:targetSdkVersion="19"/>

  <application
      android:name="com.company.tutorial.SampleApplication">

   <activity android:name="com.company.tutorial.SampleActivity">
     <intent-filter>
       <action android:name="android.intent.action.MAIN"/>
       <category android:name="android.intent.category.LAUNCHER"/>
     </intent-filter>
   </activity>

  </application>
</manifest>
```

To build the app, we need a root `BUCK` file:

``` python
android_binary(
    name = "sample-barebones",
    keystore = ":debug_keystore",
    manifest = "src/main/AndroidManifest.xml",
    deps = [
        "//src/main/java/com/company/tutorial:tutorial",
    ],
)

keystore(
    name = "debug_keystore",
    properties = "debug.keystore.properties",
    store = "debug.keystore",
)
```

Look [here](https://coderwall.com/p/r09hoq/android-generate-release-debug-keystores) for instructions on creating a debug keystore, and [here](https://github.com/{{ site.ghrepo }}/blob/master/sample/debug.keystore.properties) for a `debug.keystore.properties` file.

Now, we need some java (we referred to it in `deps` above).  In `src/main/java/com/company/tutorial` add the `BUCK` file:

``` python
android_library(
    name = "tutorial",
    srcs = glob(["**/*.java"]),
    visibility = [
        "PUBLIC",
    ],
    deps = [
        '//lib:soloader',
    ],
)
```

This buck target has all the `*.java` files in `src/main/java/com/company/tutorial` as its sources.  We just need two, a simple application, `SampleApplication.java`

``` java
package com.company.tutorial;

import android.app.Application;

import com.facebook.soloader.SoLoader;

public class SampleApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    SoLoader.init(this, false);
  }
}
```

and a simple activity, `SampleActivity.java`

``` java
package com.company.tutorial;

import android.app.Activity;

public class SampleActivity extends Activity {
}
```

Note the `SoLoader.init` in the application.  SoLoader is a library for loading native libraries, and their dependencies.  Since Litho uses [Yoga](https://facebook.github.io/yoga/) for layout (which has native dependencies), we need SoLoader.  This init step needs to be called before almost anything else, so we don't try reference unloaded libraries.

Finally, add a target in `/lib/BUCK` to actually download SoLoader

``` python
android_prebuilt_aar(
    name = "soloader",
    aar = ":soloader-aar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "soloader-aar",
    sha1 = "918573465c94c6bc9bad48ef259f1e0cd6543c1b",
    url = "mvn:com.facebook.soloader:soloader:aar:0.1.0",
)
```

Now we can build and test on an emulator with `buck fetch :sample-barebones` and `buck install -r :sample-barebones`.  With any luck, this should show a blank app!

## 2. Hello Litho World

In this part, we'll introduce Litho in a very basic way to our app.  We'll use a predefined Litho `Text` widget to display "Hello World" on the screen.  Along the way, we'll learn about the building blocks of Litho: components, the component tree, `ComponentView` and setting properties on components.

First off we need to import the library.  Add the following to your `/lib/BUCK` to fetch the package from jcenter.

``` python
android_prebuilt_aar(
    name = "litho",
    aar = ": litho-aar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho-aar",
    sha1 = "918573465c94c6bc9bad48ef259f1e0cd6543c1b",
    url = "FILL ME IN",
)
```

Then add `'/lib:litho'` to the `deps` of `src/main/java/com/company/tutorial:tutorial`.  Note that it is a dependency of the java library, and not a root dependency of the project.

User interfaces in Litho are defined in terms of components.  You will write _component specs_ which define the component, what properties it has, how it renders etc.  These specs may be written in terms of other components.  For example a `ForumPost` component might have a `Text` component for the user name, another `Text` for the message and an `Image` for the user avatar.  Litho provides a number of useful, reusable components, such as `Text` and `Image` for you to build on.  Take a look [here](https://github.com/{{ site.ghrepo }}/tree/master/src/main/java/com/facebook/components/widget) for a full list.

For this example, we'll just add a single `Text` component to the app.  In your `SampleActivity`, now override the constructor.  In this we will set up the content view of this activity.  Let's dive right in

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

Of course, you'll need to add some imports, included here for completeness:

``` java
import android.os.Bundle;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;
import com.facebook.litho.Component;
import com.facebook.litho.widget.Text;
```

Lets dissect the above a little.  The easiest to discuss is probably

``` java
Text.create(context)
        .text("Hello World")
        .textSizeDip(50)
        .build()
```

`Text` is a core component defined in `com.facebook.litho.widget`.  It has a number of _properties_ such as _text_ and _textSize_ which you can set as shown.  In fact, the class `Text` was generated from a `TextSpec` class.  We'll go into the specifics of writing your own components later.

A `ComponentView` is an android `View` and can thus be set as the content of this activity.  You add a `ComponentTree` (which manages your component's life cycle) to the view.  Components get added to the tree as shown.

Note that often this would be written as the more compact and pleasing, but slightly more confusing

``` java
final ComponentTree componentTree = ComponentTree.create(context,
        Text.create(context)
                .text("Hello World")
                .textSizeDip(50)
                .build())
        .build();
```

And that is it!  Running this app should get you something much like this

<img src="/static/images/barebones1.png" style="width: 300px;">

Not pretty, but this is certainly a start!

## 3. A First Component

The end goal of this tutorial is some sort of simple, scrollable feed.  This feed will just say "Hello World" a whole lot of times.  In this tutorial, we'll look at defining one of the elements (the "Hello World"s) that appear in the feed.  Naturally, in full scale applications, elements will be substantially more complicated components.

Let us dive in and build this component.  In Litho, these are defined by _Spec_ classes.  We'll call our component `FeedItem`.  So thus we define a class called `FeedItemSpec`.

``` java
import android.graphics.Color;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.widget.Text;
import static com.facebook.yoga.YogaEdge.ALL;

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

Some of this is familiar.  We see the `Text` component from before.  However, now we are passing it as a "child" property of a `Container`.  You can think `Container`s like `<div>`s in HTML.  Its a wrapper, usually simply used for collating things together (and some background styling).  In fact, since components uses [Yoga](https://facebook.github.io/yoga/), you can add flexbox attributes to layout the children of a `Container`.  Here, we simply set the padding and background color.

How do we use this component?  Its rather simple.  In the `SampleActivity`, simply change the `ComponentTree` definition to

``` java
final ComponentTree componentTree = ComponentTree.create(
    context,
    FeedItem.create(context))
        .build();
```

**Note** That's `FeedItem`, not `FeedItemSpec`.

Where did `FeedItem` come from?  And where are `create` and `build` defined?  This is the magic of Specs.  We need to add to our buck target `/src/main/java/com/company/tutorial:tutorial`

``` python
plugins = [
        INSERT_ANNOTATION_TARGET_HERE,
    ],
```

This runs an annotation processor over our code.  It looks for `(.*)Spec` class names and constructs `(.*)` classes.  These classes will have all the methods required by Litho automatically filled in.  In addition, based upon the specification, there will be extra methods (such as `Text`'s `.textSizeSp` or the `.backgroundColor` method of `Container`).

But that's as simple as it is.  Run and you should see

<img src="/static/images/barebones2.png" style="width: 300px;">

## 4. Feed Me!

Feeds in Litho are based upon the `Recycler` component.  This component is similar, conceptually, to the Android `RecyclerView`.  However, with Litho, all the layout is performed in a separate thread, giving a substantial performance boost.  In this part, we'll use a `RecyclerBinder` that provides component to a `Recycler`, in the same way an `LayoutMangager` and `Adapter` combination provides `View`s to a `RecyclerView`.

The first thing we need to do is add the android support recyclerview to the libs, much as we did for SoLoader in the first tutorial:

``` python
java_library(
    name = "android-support-recyclerview",
    exported_deps = [
        ":android-support-recyclerview.jar",
    ],
)

prebuilt_jar(
    name = "android-support-recyclerview.jar",
    binary_jar = "android-support-v7-recyclerview.jar",
)
```

Then, add it as a dependency of `/src/main/java/com/company/tutorial:tutorial`.  Now we will construct a `RecyclerBinder` and attach it to a `Recycler`.  A `RecyclerBinder` takes a component context, a range ratio and a layout info as constructor parameters.  For this example, simply set the range ratio to 4, and construct a `LinearLayoutInfo` for the layout info.  For more information see the detailed docs.

Adding a `Recycler` component to the component tree is as simple as any other component, and setting the binder is straightforward.

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

Now we need to populate the binder.  For this, we will define a helper function in `SampleActivity`.  Binders take `ComponentInfo` classes that describe the components to be rendered by the `Recycler`.  In this case, we want a simple `ComponentInfo` that simply presents our `FeedItem` component.

``` java
private void addContent(RecyclerBinder recyclerBinder, ComponentContext context) {
  for (int i = 0; i < 32; i++) {
    ComponentInfo.Builder componentInfoBuilder = ComponentInfo.create();
    componentInfoBuilder.component(FeedItem.create(context).build());
    recyclerBinder.insertItemAt(i, componentInfoBuilder.build());
  }
}
```

It's that simple.  Call `addContent` somewhere in the main activity `onCreate` and running the app gives a scrollable list of 32 "Hello World" components:

<img src="/static/images/barebones3.png" style="width: 300px;">

## 5. Properties

Feeds are no good if they only contain repetitive copies of a single component.  In this part, we will look at _properties_: attributes you can set on components you define in order to change their behavior or appearance.

Adding properties to a component is very simple.  Properties are simply parameters to methods of the specification, annotated with the `@Prop` annotation.  Lets add some properties to our `FeedItem` that will change the appearance of the component.  We'll add a `message` and `color` property.

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

The magic is in the `@Prop` annotations and the annotation processor.  The processor produces methods on the builder that correspond to the properties in a smart way.  Thus, we simply change the binder's construction of the component to

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

The only change is in lines 5 to 8.  This gives

<img src="/static/images/barebones4.png" style="width: 300px;">

You can specify more options to the `@Prop` annotation.  For example, consider the property

``` java
@Prop(optional = true, resType = ResType.DIMEN_OFFSET) int shadowRadius,
```

This tells the annotation processor to construct a number of functions, such as `shadowRadiusPx`, `shadowRadiusDip`, `shadowRadiusSp` as well as `shadowRadiusRes`.  For more information, see the full documentation.

This concludes the bare bones tutorial, and the code for the finished product can be found [here](https://github.com/facebook/c4a/blob/master/sample-barebones/).  For more in-depth code, check out the [sample](https://github.com/facebook/c4a/blob/master/sample/), as well as the documentation elsewhere on this website.
