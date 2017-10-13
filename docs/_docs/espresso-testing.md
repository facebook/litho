---
docid: espresso-testing
title: Espresso
layout: docs
permalink: /docs/espresso-testing
---

Litho provides basic support for the
[Espresso UI Testing](https://developer.android.com/training/testing/ui-testing/espresso-testing.html)
framework. For the time being, we only provide shallow selectors that will match `ComponentHost`s and
`LithoView`s to run very high-level tests against. If you find that too limiting, please
[reach out to us](https://github.com/facebook/litho/issues/new) and describe your use case.

## Getting Started

<img src="/static/images/espresso-logo.png" style="width: 150px; float: right; margin-left: 50px;">

We ship with a bunch of custom matchers, which will hopefully make your life easier.

The matchers exist as their own `litho-espresso` package, so make sure to add the
dependencies to your `build.gradle` file alongside your Espresso setup:

```gradle
dependencies {
  // ...
  androidTestCompile 'com.facebook.litho:litho-espresso:{{site.litho-version}}'
  androidTestCompile 'com.android.support.test.espresso:espresso-core:2.2.2'
  androidTestCompile 'com.android.support.test.espresso:espresso-intents:2.2.2'
}
```

<br style="clear: both; overflow: hidden;">

## Matching

In order to instruct Litho to record where elements are mounted and give you access to
`testKeys` you can set in your `ComponentLayouts`, replace your usual `ActivityTestRule`
with the specialized `LithoActivityTestRule`:

```java
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MyTest {
  @Rule
  public LithoActivityTestRule<MyActivity> mActivity =
      new LithoActivityTestRule<>(MyActivity.class);
      
  // ...
}
```

Afterwards, you can check against `ComponentHost`s and `LithoView`s as you normally would
for standard Android `View`s. For instance, let's assume you have a mounted
clickable component like this present in the selected activity:

```java
CustomButton.create(c)
    .text("My Button")
    .clickHandler(/* ... */)
    .build();
```

Then, you could write an Espresso test like this:

```java
@Test
public void testButtonIsDisplayedAndClickable() {
    onView(ComponentHostMatchers.componentHostWithText(containsString("My Button")))
        .check(matches(allOf(isDisplayed(), isClickable())));
}
```

Alternatively, you can match against components by using `testKey`. `testKey`s allow
you to specify a string for a Component, similar to an Android `viewTag`, but without
sacrificing view flattening. Instead, Litho maintains a separate data structure to keep
track of the keys and associated metadata, but only if the end-to-end test mode is enabled
through the use of `LithoActivityTestRule`:

```java
CustomButton.create(c)
    .text("My Button")
    .testKey("my-button")
    .clickHandler(/* ... */)
    .build();
```

```java
@Test
public void testButtonIsDisplayedAndClickableByTestKey() {
    onView(LithoViewMatchers.withTestKey("my-button"))
        .check(matches(allOf(isDisplayed(), isClickable())));
}
```

## Additional Resources

There are plenty of additional matchers available, which you can look up in the
[API docs](/javadoc/):

- [LithoViewMatchers](/javadoc/com/facebook/litho/testing/espresso/LithoViewMatchers.html)
- [ComponentHostMatchers](/javadoc/com/facebook/litho/testing/espresso/ComponentHostMatchers.html)
