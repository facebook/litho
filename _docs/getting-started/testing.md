<block class="gradle buck" />

## Testing your Installation

You can test your install by adding a view created with Litho to an activity.

First, initialize `SoLoader`. Litho has a dependency on [SoLoader](https://github.com/facebook/SoLoader) to help load native libraries provided by the underlying layout engine, [Yoga](https://facebook.github.io/yoga/). Your `Application` class is a good place to do this:

```java
[MyApplication.java]
public class MyApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, false);
  }
}
```

Then, add a predefined Litho `Text` widget to an activity that displays "Hello World!":

```java
[MyActivity.java]
public class MyActivity extends Activity {
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

> The class `Text` lives in the package `com.facebook.litho.widget`.  All other classes in the above are from `com.facebook.litho`.

Run the app, you should see "Hello World!" displayed on the screen.
