# 1. Overview

In this codelab, you'll learn about events: how to declare them, bind them to components, react to events and creating custom events.

### Prerequisites:
- `LayoutSpec`s
- State Updates

# 2. Setup the environment

Clone the following repo: [https://github.com/facebook/litho/tree/master/codelabs/events](https://github.com/facebook/litho/tree/master/codelabs/events)

# 3. Add `ClickEvent` and show message

In this part we will see how to add `ClickEvent` declaration and corresponding handler to the component. The screen will contain "ADD" button and it should show `Toast` message when clicked.

## Add button component
First, let's create a separate component for button so that we can reuse it later. In our case button is just a styled `Row` containing `Text` component:
#### ButtonSpec.kt
```kotlin 
@LayoutSpec
object ButtonSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop text: String): Component {
    return Row.create(c, 0, R.style.Widget_AppCompat_Button_Small)
        .clickable(true)
        .child(
            Text.create(c)
                .alignSelf(YogaAlign.CENTER)
                .textSizeSp(20f)
                .text(text))
        .build()
  }
}
```

## Define `ClickEvent`
Now, let's switch to main component to define our layout. We add our `Button` component and align it horizontally to the center.

#### RootComponentSpec.kt
```kotlin
@OnCreateLayout
fun onCreateLayout(c: ComponentContext): Component {
  return Column.create(c)
      .paddingDip(YogaEdge.ALL, 20f)
      .child(
          Button.create(c)
              .alignSelf(YogaAlign.CENTER)
              .text("ADD"))
      .build()
}
```

To declare event in your component you need to add a function annotated with `@OnEvent`. That annotation accepts the class value of event type, and here we will use predefined event type for clicks: `ClickEvent`. The function will accept `ComponentContext` and any prop or state as params. We will show `Toast` message when click event is triggered:

#### RootComponentSpec.kt
```kotlin
@OnEvent(ClickEvent::class)
fun onClickEvent(c: ComponentContext) {
  Toast.makeText(c.androidContext, "ADD button clicked", Toast.LENGTH_SHORT).show()
}
```

## Add click handler to component

We now have an event declaration which on its own does not do anything. We need to bind that event to the component that will trigger it. In our case we need to add `.clickHandler` to our `Button` component and it accepts `EventHandler<ClickEvent>` type. In order to add event handler we need to first compile our code so that the generated component (`RootComponent`) contains event handler function that points to `onClickEvent` we declared earlier. We need this level of indirection so that the generated component can prefill `Prop` and `State` values that could have been declared in `onClickEvent` and provide a method where you only pass `ComponentContext`:

#### RootComponentSpec.kt
```kotlin
Button.create(c)
    .alignSelf(YogaAlign.CENTER)
    .text("ADD")
    .clickHandler(RootComponent.onClickEvent(c)))
```  

## Click event in action

We have now all pieces together and the interaction looks like the following:

![Part1 end result](static/part1_endresult.png)

The next step will be to trigger state update and perform some action in response to click.

# 4. Trigger state update on click event

In previous part we added simple click event declaration and handler. In this part we will add state to the root component and use click event to trigger state update.

## Add `ColorBoxCollection` component
To demonstrate some visual change as response to click event, we will create a new component called `ColorBoxCollection`. It renders a set of square color boxes laid out in rows. It will accept a list of colors that it will turn into corresponding square color boxes.

#### ColorBoxCollectionSpec.kt
```kotlin
@LayoutSpec
object ColorBoxCollectionSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop items: IntArray): Component {
    val rowBuilder = Row.create(c).wrap(YogaWrap.WRAP)
    items.forEach {
      rowBuilder.child(
          Row.create(c)
              .marginDip(YogaEdge.ALL, 4f)
              .widthDip(48f)
              .heightDip(48f)
              .backgroundColor(it))
    }
    return rowBuilder.build()
  }
}
```
## Introduce state for list of colors

To keep track of added colors we will introduce new state `items` of type `IntArray`. Let's add initialization and update method:

#### RootComponentSpec.kt
```kotlin
@OnCreateInitialState
fun onCreateInitialState(c: ComponentContext, items: StateValue<IntArray>) {
  items.set(IntArray(0))
}

@OnUpdateState
fun updateItems(items: StateValue<IntArray>, @Param newItems: IntArray) {
  items.set(newItems)
}
```


## Add new component into layout of `RootComponent`

In `RootComponent` we will place `ColorBoxCollection` just below our "ADD" button. We will use the `items` state value to pass to `ColorBoxCollection`:

#### RootComponentSpec.kt
```kotlin
@OnCreateLayout
fun onCreateLayout(c: ComponentContext, @State items: IntArray): Component {
  return Column.create(c)
    ...
    .child(
        Button.create(c)
            ...
    .child(
        ColorBoxCollection.create(c)
            .items(items))
    .build()
}

```

## Generate new color and update state with new list on click

Now we can update the body of `onClickEvent` to generate new color and trigger state update method we introduced earlier. To generate new color we generate three random values and assign to each of RGB channels. As we want props and state to be immutable we get a new copy of array with new item added:

#### RootComponentSpec.kt
```kotlin
@OnEvent(ClickEvent::class)
fun onClickEvent(c: ComponentContext, @State items: IntArray) {
  val newColor = Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
  RootComponent.updateItems(c, items.plus(newColor))
}
```

## Click event in action

With all pieces together here is how it looks:

<img src="static/part2_endresult.gif" alt="Part2 end result" height="500">

The next step will be to add param to the event method.


# 5. Add `@Param` to `OnEvent`

# 6. Add custom event

# 7. Add params to custom event

# 8. Add visible and invisible events

# 9. Summary


