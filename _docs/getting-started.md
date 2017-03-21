---
docid: getting-started
title: Getting Started
layout: docs
permalink: /docs/getting-started.html
---

## Installing Litho

ADD INSTALLATION INSTRUCTIONS HERE

## Verifying Installation with Hello World

Hello, World in Litho is relatively simple.  A full walkthrough, including setting up libraries and annotation processors required for full functionality can be found [here](/docs/tutorial.html/).

However, as a taster, this is a simple "Hello, World!" activity using Litho.  It constructs a `ComponentContext`, a `ComponentView` and a `ComponentTree`, and displays a text box to the screen.

``` java
import android.app.Activity;
import android.os.Bundle;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentInfo;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;
import com.facebook.litho.widget.Text;

public class SampleActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentView componentView = new ComponentView(this);
    final ComponentContext context = new ComponentContext(this);

    final Component text = Text.create(context)
        .text("Hello, World!")
        .build();
    final ComponentTree componentTree = ComponentTree.create(context, text).build();

    componentView.setComponent(componentTree);

    setContentView(componentView);
  }
}
```
