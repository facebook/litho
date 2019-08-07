---
docid: dynamic-props
title: Dynamic Props
layout: docs
permalink: /docs/dynamic-props
---

Normally, when the value of a `@Prop` within a `ComponentTree` changes, the framework needs to compute layout and mount the `Component` again.
However, there is a category of `@Props` that do not affect layout, thus when the value of the `@Prop` changes, the framework can take a "shortcut": apply the new value to the mounted UI element that represents the `Component` right away.
We call such properties *"dynamic"*.
[`DynamicValue<T>`](/javadoc/com/facebook/litho/DynamicValue) is the interface that makes it possible.

### Common Dynamic Props

The dynamic properties that are available for **all** `Components` are:
* Alpha
* Scale X/Y
* Translation X/Y
* Background Color

To use this, all you need to do is to create and pass a `DynamicValue<T>` object to the corresponding [`Component.Builder`](/javadoc/com/facebook/litho/Component.Builder) method.
Normally, you would hold on to this object, and use its [`set()`](/javadoc/com/facebook/litho/DynamicValue.html#set-T-) method to update the actual value.

In the following sample we have a `Component` that renders a yellow square in the middle of the screen.
We also have two regular Android `SeekBars` "outside" of the `Components` hierarchy that control the alpha and the scale levels of the square.

<video loop autoplay class="video" style="width: 100%; height: 500px;">
  <source type="video/webm" src="/static/videos/common_dynamic_props.webm"></source>
  <p>Your browser does not support the video element.</p>
</video>

```java
/**
 * MyComponentSpec.java
 */
@LayoutSpec
public class MyComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop DynamicValue<Float> alphaDV,
      @Prop DynamicValue<Float> scaleDV) {
    Component yellowSquare = Rect.create(c)
                .color(YELLOW)
                .alpha(alphaDV)
                .scaleX(scaleDV)
                .scaleY(scaleDV)
                .build();

    return Column.create(c)
        .child(yellowSquare)
        .alignItems(YogaAlign.CENTER)
        .justifyContent(YogaJustify.CENTER)
        .build();
  }
}

/**
 * MyActivity.java
 */
public class MyActivity extends Activity
    implements SeekBar.OnSeekBarChangeListener {

  private DynamicValue<Float> mAlphaDV;
  private DynamicValue<Float> mScaleDV;

  private TextView mAlphaLabel;
  private TextView mScaleLabel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mAlphaDV = new DynamicValue<>(1f);
    mScaleDV = new DynamicValue<>(1f);

    setContentView(R.layout.activity);

    ComponentContext c = new ComponentContext(this);
    Component component = MyComponent.create(c)
			.alphaDV(mAlphaDV)
			.scaleDV(mScaleDV)
			.build();

    LithoView lithoView = findViewById(R.id.lithoView);
    lithoView.setComponent(component);

    mAlphaLabel = findViewById(R.id.alphaValue);
    mScaleLabel = findViewById(R.id.scaleValue);

    SeekBar alphaSeekBar = findViewById(R.id.alphaSeekBar);
    alphaSeekBar.setMax(100);
    alphaSeekBar.setProgress(100);
    alphaSeekBar.setOnSeekBarChangeListener(this);

    SeekBar scaleSeekBar = findViewById(R.id.scaleSeekBar);
    scaleSeekBar.setMax(150);
    scaleSeekBar.setProgress(50);
    scaleSeekBar.setOnSeekBarChangeListener(this);
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    if (seekBar.getId() == R.id.alphaSeekBar) {
      // Update alpha value and label
      float alpha = progress / 100f;
      mAlphaDV.set(alpha);
      mAlphaLabel.setText(Float.toString(alpha));
    } else {
      // Update scale value and label
      float scale = (progress + 50) / 100f;
      mScaleDV.set(scale);
      mScaleLabel.setText(Float.toString(scale));
    }
  }
}
```

Notice that:
1. On *lines 43-44*, in `MyActivity.java`, we create `DynamicValue` objects
2. On *lines 50-51*, in `MyActivity.java`, we supply the `DynamicValues` to the `MyComponent` (just as regular `@Props`).
3. On *lines 14-16*, in `MyComponentSpec.java`, we pass `DynamicValue<Float>` objects to `alpha()`, `scaleX()` and `scaleY()` methods of `Component.Builder` to control the corresponding properties of the `Rect` component.
4. On *lines 76, 81*, in `MyActivity.java`, we use the `DynamicValue` objects to keep the state of the `SeekBars` and the value of the properties they control in sync.

### Custom Dynamic Props for MountSpecs

You may have your own `MountSpec` which has `@Props` that also do not affect layout.
It is possible to control the values of those properties in similar way using `DynamicValue` interface.
However, in this case you will need to tell framework *how* to apply the value of the `@Prop` to the mounted element.

To show you how to do this, let us consider a `MountSpec` that mounts a [`ClockView`](https://github.com/facebook/litho/blob/master/sample/src/main/java/com/facebook/samples/litho/dynamicprops/ClockView.java) and defines `time` property, which it passes to the View in `@OnMount`.

```java
/**
 * ClockComponentSpec.java
 */
@MountSpec
class ClockComponentSpec {

  @OnCreateMountContent
  static ClockView onCreateMountContent(Context c) {
    return new ClockView(c);
  }

  @OnMount
  static void onMount(ComponentContext c, ClockView clockView, @Prop long time) {
    clockView.setTime(time);
  }
}
```

<img src="/static/images/clock_view.png" style="height: 500px;">

Notice that the value of the `time` property does not affect layout, it only controls how the `ClockView` draws clock hands.
However, every time you want to update it the framework will have to go through `LayoutState` and `MountState`.

Here is how we can fix this by converting to Dynamic Props and, at the same time, get a more convenient interface to adjust the value.

```java
/**
 * ClockComponentSpec.java
 */
@MountSpec
class ClockComponentSpec {

  @OnCreateMountContent
  static ClockView onCreateMountContent(Context c) {
    return new ClockView(c);
  }

  @OnBindDynamicValue
  static void onBindTime(
	  ClockView clockView,
	  @Prop(dynamic = true) long time) {
    clockView.setTime(time);
  }
}
```

**First thing** you need to do is to mark the `@Prop` as dynamic - *line 15*.
Once you have done this, the framework will generate an additional method to the builder of your `Component` that takes a `DynamicValue`.
At the same time it will keep the version of this method that takes "static" value, if you choose to use this in some situations.

```java
/**
 * ClockComponent.java (generated)
 */
 public final class ClockComponent extends Component {
   ...
   public static final class Builder extends Component.Builder<Builder> {
      ...
      public Builder time(DynamicValue<Long> time) {...}
      public Builder time(long time) {...}
      ...
   }
}
```
**Second thing** is to create a [`@OnBindDynamicValue`](/javadoc/com/facebook/litho/annotations/OnBindDynamicValue) method - *lines 12-17* in `ClockComponentSpec.java` - that should set the value to the mounted content.
This method should always takes 2 arguments - mounted content, and the `@Prop` itself. You need to create one such method for every dynamic `@Prop` you define.
Then, it is the responsibility of the framework to invoke these methods to keep changes to the `DynamicValue`.

<video loop autoplay class="video" style="width: 100%; height: 500px;">
  <source type="video/webm" src="/static/videos/custom_dynamic_props.webm"></source>
  <p>Your browser does not support the video element.</p>
</video>

[Here](https://github.com/facebook/litho/tree/master/sample/src/main/java/com/facebook/samples/litho/dynamicprops) you find the full implementation of the sample above.