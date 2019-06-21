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

*Under construction*

