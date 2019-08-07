---
docid: widgets
title: Widgets
layout: docs
permalink: /docs/widgets
---
Litho provides a number of build-in components.
You can find the full list of components and APIs within our [javadocs for the com.facebook.litho.widget package](https://fblitho.com/javadoc/index.html?com/facebook/litho/widget/package-summary.html).

We'll show and explain here a list of the most basic widgets.


Font Size:
bold italic
Size
Vertical Size:
Horizontal Size:
Border
Border Radius:
Border Size:
Box Shadow
Text Shadow
Vertical Position:
Horizontal Position:
Blur Radius:

<a href="#text"
  style="background-color:#f36b7f; -moz-border-radius:20px;	-webkit-border-radius:20px;	border-radius:20px; display:inline-block;	color:#ffffff; font-family:Arial; font-size:20px; padding:14px 30px; margin-bottom: 10px; margin-top: 10px; margin-right: 6px; text-decoration:none;"
  onMouseOver="this.style.backgroundColor='#f5445f'"
  onMouseOut="this.style.backgroundColor='#f36b7f'">Text</a>  <a href="#textinput"
  style="background-color:#f36b7f; -moz-border-radius:20px;	-webkit-border-radius:20px;	border-radius:20px; display:inline-block;	color:#ffffff; font-family:Arial; font-size:20px; padding:14px 30px; margin-bottom: 10px; margin-top: 10px; margin-right: 6px; text-decoration:none;"
  onMouseOver="this.style.backgroundColor='#f5445f'"
  onMouseOut="this.style.backgroundColor='#f36b7f'">TextInput</a>  <a href="#image"
  style="background-color:#f36b7f; -moz-border-radius:20px;	-webkit-border-radius:20px;	border-radius:20px; display:inline-block;	color:#ffffff; font-family:Arial; font-size:20px; padding:14px 30px; margin-bottom: 10px; margin-top: 10px; margin-right: 6px; text-decoration:none;"
  onMouseOver="this.style.backgroundColor='#f5445f'"
  onMouseOut="this.style.backgroundColor='#f36b7f'">Image</a>  <a href="#card"
  style="background-color:#f36b7f; -moz-border-radius:20px;	-webkit-border-radius:20px;	border-radius:20px; display:inline-block;	color:#ffffff; font-family:Arial; font-size:20px; padding:14px 30px; margin-bottom: 10px; margin-top: 10px; margin-right: 6px; text-decoration:none;"
  onMouseOver="this.style.backgroundColor='#f5445f'"
  onMouseOut="this.style.backgroundColor='#f36b7f'">Card</a>  <a href="#solidcolor"
  style="background-color:#f36b7f; -moz-border-radius:20px;	-webkit-border-radius:20px;	border-radius:20px; display:inline-block;	color:#ffffff; font-family:Arial; font-size:20px; padding:14px 30px; margin-bottom: 10px; margin-top: 10px; margin-right: 6px; text-decoration:none;"
  onMouseOver="this.style.backgroundColor='#f5445f'"
  onMouseOut="this.style.backgroundColor='#f36b7f'">SolidColor</a>  <a href="#progress"
  style="background-color:#f36b7f; -moz-border-radius:20px;	-webkit-border-radius:20px;	border-radius:20px; display:inline-block;	color:#ffffff; font-family:Arial; font-size:20px; padding:14px 30px; margin-bottom: 10px; margin-top: 10px; margin-right: 6px; text-decoration:none;"
  onMouseOver="this.style.backgroundColor='#f5445f'"
  onMouseOut="this.style.backgroundColor='#f36b7f'">Progress</a>  <a href="#spinner"
  style="background-color:#f36b7f; -moz-border-radius:20px;	-webkit-border-radius:20px;	border-radius:20px; display:inline-block;	color:#ffffff; font-family:Arial; font-size:20px; padding:14px 30px; margin-bottom: 10px; margin-top: 10px; margin-right: 6px; text-decoration:none;"
  onMouseOver="this.style.backgroundColor='#f5445f'"
  onMouseOut="this.style.backgroundColor='#f36b7f'">Spinner</a>  <a href="#verticalscroll"
  style="background-color:#f36b7f; -moz-border-radius:20px;	-webkit-border-radius:20px;	border-radius:20px; display:inline-block;	color:#ffffff; font-family:Arial; font-size:20px; padding:14px 30px; margin-bottom: 10px; margin-top: 10px; margin-right: 6px; text-decoration:none;"
  onMouseOver="this.style.backgroundColor='#f5445f'"
  onMouseOut="this.style.backgroundColor='#f36b7f'">VerticalScroll</a>  <a href="#horizontalscroll"
  style="background-color:#f36b7f; -moz-border-radius:20px;	-webkit-border-radius:20px;	border-radius:20px; display:inline-block;	color:#ffffff; font-family:Arial; font-size:20px; padding:14px 30px; margin-bottom: 10px; margin-top: 10px; margin-right: 6px; text-decoration:none;"
  onMouseOver="this.style.backgroundColor='#f5445f'"
  onMouseOut="this.style.backgroundColor='#f36b7f'">HorizontalScroll</a>  <a href="#recycler"
  style="background-color:#f36b7f; -moz-border-radius:20px;	-webkit-border-radius:20px;	border-radius:20px; display:inline-block;	color:#ffffff; font-family:Arial; font-size:20px; padding:14px 30px; margin-bottom: 10px; margin-top: 10px; margin-right: 6px; text-decoration:none;"
  onMouseOver="this.style.backgroundColor='#f5445f'"
  onMouseOut="this.style.backgroundColor='#f36b7f'">Recycler</a>

## Text

This is the most basic Litho component to show simple text. It's the equivalent of an Android `TextView` within Litho.

##### Required Props
- `CharSequence text`: Text to display.

#### Usage
`Text` has numerous optional props you can use to style your text, same as `TextView` since both use `android.text.Layout` under the hood. A full list of them is available in the [javadocs](https://fblitho.com/javadoc/com/facebook/litho/widget/Text.html).
Most props directly accept resources ids too.

```java
final Component component =
    Text.create(c)
        .text("This is my example text")
        .textSizeRes(R.dimen.my_text_size)
        .textColorRes(R.color.my_text_color)
        .textAlignment(Layout.Alignment.ALIGN_CENTER)
        .build()
 ```


## TextInput

 Component that renders an editable text input using an Android `EditText`.

##### Required Props
- None.

#### Usage
Because this component is backed by Android's `EditText`, many native capabilities are applicable!
- Use an `android.text.InputFilter ` to set a text length limit or modify text input.
- Change the input representation by passing an `android.text.InputType` constants.
- For performance reasons, avoid re-creating the Component with different props to change its configuration. You can instead use Event triggers `OnTrigger` to update text, request view focus or set selection. e.g. `TextInput.setText(context, "myTextInputKey", "myText")`.

```java
private static final InputFilter lenFilter = new InputFilter.LengthFilter(maxLength);
Component component =
    TextInput.create(c)
        .initialText(text)
        .textColorStateList(ColorStateList.valueOf(color))
        .multiline(true)
        .inputFilter(lenFilter)
        .backgroundColor(Color.TRANSPARENT)
        .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        .build()
 ```


## Image

A component that is able to display drawable resources.

##### Required Props
- `Drawable drawable`: Drawable to display.

#### Usage
```java
Component component =
    Image.create(c)
        .drawableRes(R.drawable.my_drawable)
        .scaleType(ImageView.ScaleType.CENTER_CROP)
        .build()
```


## Card

The Litho equivalent of an Android `CardView`. `Card` applies borders with shadows/elevation to a given component.
If your card is rendered on top of a dynamically colored background which cannot be "faked" using the `Card` component, check out the less performant [`TransparencyEnabledCard`](https://fblitho.com/javadoc/com/facebook/litho/widget/TransparencyEnabledCard.html).

##### Required Props
- `Component content`: The component to decorate.

#### Usage
```java
Component component =
	  Card.create(c)
        .content(myContentComponent)
        .clippingColorRes(R.color.my_clipping_color)
        .cornerRadiusDip(8)
        .build()
```


## SolidColor

A simple Component to render solid color.

##### Required Props
- `int color`: Color to display.

#### Usage
```java
Component component =
    SolidColor.create(c)
        .color(Color.RED)
        .alpha(0.5)
        .build()
```


## Progress

Renders an infinitely spinning progress bar backed by the Android's `ProgressBar`.

##### Required Props
- None.

#### Usage
```java
Component component =
	  Progress.create(c)
        .indeterminateDrawableRes(R.drawable.my_loading_spinner)
        .build()
```


## Spinner

A simple spinner (dropdown) component. Derived from the standard Android `Spinner`.

##### Required Props
- `List<String> options`: List of possible options to select from.
- `String selectedOption`: Currently selected option.

#### Usage
```java
List<String> myOptions = ...
Component component =
	  Spinner.create(c)
        .options(myOptions)
        .selectedOption(myOptions.get(0))
        .build()
```


## VerticalScroll

Component that wraps another component, allowing it to be vertically scrollable. It's analogous to Android's `ScrollView`.

##### Required Props
- `Component childComponent`: Component to vertically scroll.

#### Usage
```java
Component component =
	  VerticalScroll.create(c)
        .childComponent(myComponentToScroll)
        .verticalFadingEdgeEnabled(true)
        .fadingEdgeLengthDip(FADING_EDGE_LENGTH_DP)
        .build()
```


## HorizontalScroll

Component that wraps another component, allowing it to be horizontally scrollable. It's
analogous to Android's `HorizontalScrollView`.

##### Required Props
- `Component contentProps`: Component to horizontally scroll.

#### Usage
```java
Component component =
	  HorizontalScroll.create(c)
        .contentProps(myComponentToScroll)
        .build()
```


## Recycler

`Recycler` is the equivalent of Android's `RecyclerView`. We suggest you to use [Sections](/docs/sections-intro) for efficient lists rendering, which is using `Recycler` under the hood.
However, if you really want to use `Recycler` directly, we have an [advanced guide dedicated to it](/docs/recycler-component).
