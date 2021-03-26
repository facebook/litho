---
id: updating-ui
title: Updating the UI
---

## Components and immutability
Conceptually, components are like pure functions. They accept arbitrary immutable inputs (called “props”) and return a description of the layout that should appear on the screen.
Components are immutable objects and the framework views the props of a component as read-only attributes. Once a component is created, its props cannot be mutated during its lifecycle without breaking the assumption that components must be immutable for [asynchronous layout](asynchronous-layout) to be performed safely and correctly.

Of course, application UIs are dynamic and change over time as a result of network changes or user input, so you need a way of informing the framework when these changes occur. So without mutating the props, how do you update a component?
There are two ways of updating the UI: passing new [props](props) or updating the internal [state](/docs/mainconcepts/coordinate-state-actions/state-overview) of a component.

Every time something needs to be updated on screen, the framework will recreate the ComponentTree that represents the UI, which will be made up of new instances of Components created with the new data values that reflect the desired changes.

The code samples below are extracted from the [codelab on updating UI](https://github.com/facebook/litho/tree/master/codelabs/updating-ui).

## Passing new props
The data flow in a Litho components hierarchy is top-down. All the props that a component receives are passed down from its parent when it's created and cannot be changed afterwards.
That means that in order to update a component's props the framework needs to recreate the tree of components starting from the root; this translates to creating a new component with the new prop values and setting it as a root to an existing ComponentTree.

Updating the UI by passing new props is commonly used in cases when the update happens as a result of data changing from outside the scope of a component - usually network changes.

Let's see what this looks in practice. In the codelab example, we'll use a Text component that displays a string passed in through the `labelText` prop.
Think of this string as data coming from the server, which the Text component has no control over - it simply displays the input string.

```java
@LayoutSpec
object RootComponentSpec {

    @OnCreateLayout
    fun onCreateLayout(c: ComponentContext, @Prop labelText: String): Component {
        return Column.create(c)
          .child(Text.create(c).textSizeSp(20f).text(labelText))
          .build()
    }
}
```
To render this component we create a LithoView in our activity and pass it as root.
We also hold on to this LithoView, so when we want to update the UI we don't create and attach a new LithoView, we simply set a new root on the view we already have.
The first time we set the root to the LithoView, the framework will calculate layout synchronously on the UI thread, so that the UI is ready to be displayed as fast as possible.

```java
val lithoView = LithoView.create(
      this,
      RootComponent.create(componentContext)
        .labelText("Starting countdown")
        .build()
    )

setContentView(
   lithoView
)
```

In the example we start a timer which periodically changes the string on the label (in practice, this could mean starting a network request and waiting for the response).
To update the string on the label, we create a new RootComponent with the update value for `labelText` prop and set it as the new root of our LithoView.

```java
val timer = object: CountDownTimer(30000, 1000) {
  override fun onTick(millisUntilFinished: Long) {
    lithoView.setComponentAsync(
      RootComponent.create(componentContext)
        .labelText("Ms until finished: " + millisUntilFinished.toString())
        .build())
  }

  override fun onFinish() {
    lithoView.setComponentAsync(
      RootComponent.create(componentContext)
        .labelText("Done!")
        .build())
  }
}
```

Under the hood, when `lithoView.setComponentAsync` is called, the framework triggers a new layout calculation which will recreate the underlying ComponentTree with new Component instances based on the new data.
The `@OnCreateLayout` methods for all the Components in the hierarchy will be invoked again - basically calling the pure function that the component represents with new params corresponding to the new prop values.

:::caution IMPORTANT
 One important thing to note is that we are using the async option for setting a new root component (`setComponentAync`) - which tells the framework to perform the layout calculation on a background thread.
:::

For updating UI it's strongly recommended to always use the async methods - this will make your app feel more responsive. If you set a new root synchronously from the UI thread (by calling `setComponent`) the layout computation will be posted to be executed on the UI thread, which is rarely necessary.

## Updating state
Let's take a simple example of a Component which may need to update without any external factors triggering this - a toggle.
A toggle component will internally maintain a click handler which needs to update the UI to reflect the new toggle state when the user interacts with it.
Updating the toggle through props as we've seen in the example above is not a pretty thing to do. When the toggle component receives the click event, it needs to propagate this information all the way outside of the component hierarchy so that a new root component can be set with the new value of the toggle state.
This is problematic because it makes our toggle component not reusable - every time we want to add it to the UI, all the components in the hierarchy above it need to add the toggle state as a prop so that they can propagate the value down when there is a change.

Luckily, the framework exposes an API which maintains component encapsulation and hides all this nastiness away: component [state](/docs/mainconcepts/coordinate-state-actions/state-overview).
Components can maintain internal state data and trigger updates when necessary. The state data is private to the component and cannot be accessed by any other component in the hierarchy.

```
@LayoutSpec
object RootComponentSpec {

    @OnCreateInitialState
    fun onCreateInitialState(c: ComponentContext, enabled: StateValue<Boolean>) {
      enabled.set(true)
    }

    @OnCreateLayout
    fun onCreateLayout(c: ComponentContext, @State enabled: Boolean): Component {
        return Column.create(c)
          .child(Row.create(c)
            .child(Text.create(c).textSizeSp(20f).text("Toggle state: ").marginPx(YogaEdge.RIGHT, 30))
            .child(Text.create(c).textSizeSp(20f).text(enabled ? "On" : "Off").marginPx(YogaEdge.RIGHT, 30).clickHandler(RootComponent.onClick(c))))
          .build()
    }

    @OnEvent(ClickEvent::class)
    fun onClick(c: ComponentContext) {
      RootComponent.increaseCounter(c)
    }

    @OnUpdateState
    fun increaseCounter(counter: StateValue<Boolean>) {
      if (counter.get() != null && counter.get() is Boolean) {
        val counterVal: Boolean = counter.get() as Boolean
        counter.set(!counterVal)
      }
    }
}
```

The [state docs](/docs/mainconcepts/coordinate-state-actions/state-overview) describe this API in more detail.
One important thing to note is that under the hood, for the performance of your app setting new props and updating state are not much different - the framework will always recreate the entire ComponentTree with new Component instances.
The same observations about sync and async operations as for setting a new component root apply when updating state.
