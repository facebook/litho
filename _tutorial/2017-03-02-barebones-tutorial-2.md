---
title: Barebones Tutorial Part 2/5
layout: tutorial_post
author: rspencer
category: tutorial
---

## Hello Litho World

In this tutorial, we'll introduce Litho in a very basic way to our app.  We'll use a predefined Litho `Text` widget to display "Hello World" on the screen.  Along the way, we'll learn about the building blocks of Litho: components, the component tree, `ComponentView` and setting properties on components.

<!--truncate-->

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

User interfaces in Litho are defined in terms of components.  You will write _component specs_ which define the component, what properties it has, how it renders etc.  These specs may be written in terms of other components.  For example a `ForumPost` component might have a `Text` component for the user name, another `Text` for the message and an `Image` for the user avatar.  Litho provides a number of useful, reusable components, such as `Text` and `Image` for you to build on.  Take a look [here][components_widgets] for a full list.

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

[components_widgets]: https://github.com/{{ site.ghrepo }}/tree/master/src/main/java/com/facebook/components/widget
