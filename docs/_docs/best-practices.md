---
docid: best-practices
title: Best Practices
layout: docs
permalink: /docs/best-practices
---

## Coding Style

#### Guidelines: ####

 * Name your specs consistently with *NAMEComponentSpec* to generate a component called *NAMEComponent*.
 * The *ComponentContext* argument should be simply called `c` to make your layout code less verbose and more readable.
 * Use resource types (`ResType.STRING`, `ResType.COLOR`, `ResType.DIMEN_SIZE`, etc) where appropriate to make it easier to set prop values from Android resources.  
 * Declare all required props first then optional ones (`optional = true`).
 * Declare common props (props defined for all Components on `Component.Builder`) after the component's own props. 
 * Use static imports on all layout enums (`YogaEdge`, `YogaAlign`, `YogaJustify`, etc) to reduce your layout code and make it more readable.
 * Lifecycle methods, such as `@OnCreateLayout`, are static and package-private.
 * Use inline conditionals on optional children to keep the layout construction code fluent if possible.
 * If you are constructing a child container, add the container in the following line. This gives the code a layout like construction.

Here is some sample code with our styling guidelines:

```java
@LayoutSpec
class MyComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop(resType = STRING) String title,
      @Prop(optional = true) Drawable image) {

  return Row.create(c)
      .alignItems(CENTER)
      .paddingRes(R.dimen.some_dimen)
      .child(
          Image.create(c)
              .drawable(image)
              .width(40)
              .height(40)
              .marginRes(RIGHT, R.dimen.my_margin))
      .child(TextUtils.isEmpty(title) ? null :
          Text.create(c)
              .text(title)
              .textColorAttr(R.attr.textColorTertiary)
              .marginDip(5)
              .flexGrow(1f))
      .build();
  }
}
```


## Props vs State

Litho components have two types of data models: [Props](/docs/props) and [State](/docs/state). It's important to understand the difference between the two to know when you need to use each of them.

Props are for passing data down the tree from a component to its children. They are useful for defining the static part of a component's model, because props cannot be changed.

State is reserved mostly for handling updates that result from interactions with a component or updates that can only be intercepted by that component. It is managed by the component and it's not visible outside of it; a component's parent has no knowledge of its children's state.

Here's a quick overview of how to decide whether you should use props or state for some data on your component:

* Does it define a property that remains unchanged? If so, it should be a prop.
* Does it need to be initialized and passed down from the parent? If so, it should be a prop.
* Should it change its value when a user interacts with the components (for instance clicking it)? If so, it should be a state.
* Can you compute its value based on other existing props and state? If so, you shouldn't create a state for it.

Making your components stateful increasing the complexity of your application and it makes it much harder maintain and  to reason about the data flow than the top-down props approach. You should try and keep using state for your components to a minimum and strive to keep your data flow top-down. If you have multiple sibling components whose state is co-dependent, you should identify a parent component which can instead hold and manage this state for its children.

Let's take the example of a list of radio buttons, where you cannot have multiple items checked at the same time. The state of all the radio buttons depends on whether other items get clicked and checked. Instead of making all the radio buttons stateful, you should keep the state of what item is clicked in the parent and propagate that information top-down to the children through props.

## Immutability

Components are esentially functions that receive data as parameters and are immutable. When the props or state of a component change, the framework will create a new component instance with the updated information, because the previous component cannot be mutated.

While the component itself is immutable, it is easy to make it not thread safe by using mutable objects for props and state. Litho computes [layout on a background thread](/docs/asynchronous-layout), and if the objects that make up a component's props or state are mutated from another thread, then this may result in rendering different outputs for the same component.

You must keep your props and state either as primitive types which are inherently immutable, or if that's not possible make sure that the objects that you are using are immutable.
