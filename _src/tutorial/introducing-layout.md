---
id: introducing-layout
title: Introducing Layout
---
import useBaseUrl from '@docusaurus/useBaseUrl';

In this section of the tutorial, you'll become familiar with building layouts using [Flexbox](../kotlin/flexbox-containers.mdx) in Litho for an Instagram post component.

## Yoga and Flexbox

To **measure** and **layout** on-screen components, Litho uses the [Yoga](https://yogalayout.com/) library, which is an implementation of [Flexbox](https://www.w3.org/TR/css-flexbox-1/).

To learn more about Flexbox, see the [Layout System with Flexbox](../mainconcepts/flexbox-yoga.mdx) page in the 'Main Concepts' section.

## Preparatory data model classes

Before you start with the main content of this section of the tutorial, add two simple classes that represent the 'User' and 'Post' data models.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/model/models.kt start=start_example end=end_example
```

## The PostComponent

With the data models prepared, it's time to build a component for a post that features two images and one item of text laid out in a tabular format.

For now, as far as layout is concerned, it's enough to know that the component uses **Column** and **Row** (known as 'containers'). These containers are used to arrange the images and text (known as 'primitives') vertically and horizontally, respectively.

With the containers and primitives in mind, a `Post` component that renders the UI of an Instagram post look as follows:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostComponent.kt start=start_example end=end_example
```

Though the components are placed in a defined tabular format, the layout doesnt look good. So, as a final touch, you can make it look more stylish with a Flexbox style.

## Applying a Flexbox style

The following code shows the effect of using a Flexbox style on the Post component (notice the additional props for `width`, `height`, `padding`, and `margin`):

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostStyledKComponent.kt start=start_example end=end_example
```

With the Flexbox style applied, your component should look like the following image.

<img src={useBaseUrl("/images/simple_component_tutorial.png")} width="200px" className="white-background"/>

### Key Points in PostStyledKComponent.kt

* `Column` and `Row` - the key container types in Litho.
* `padding` and `margin` - props that set padding and margin using `Dimen` (a class that represents dimension values in different Android units: 'px', 'dp', and 'sp').
* `drawableRes` - returns an Android Drawable from resource ID.
* `Image` - image is another built-in component that can render a drawable.

## What next?

In the next section of the tutorial, [Adding State](adding-state.md), you'll learn about 'useState', one of Litho's 'hooks'.

For more information, see [Layout System with Flexbox](../mainconcepts/flexbox-yoga.mdx) in the 'Main Concepts' section.
